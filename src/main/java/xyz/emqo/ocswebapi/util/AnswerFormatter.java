/**
 * @Author: Emqo
 * @Date: 2026/11/09
 * @FileName: AnswerFormatter
 * @Description: 把模型返回的答案清洗成 OCS 可直接使用的纯文本
 */
package xyz.emqo.ocswebapi.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 答案格式清洗工具
 *
 * <p>OCS 不渲染 Markdown, 且会把 # 视为多答案分隔符, 因此模型输出中的
 * **加粗**、# 标题、列表、代码块、换行等内容必须去掉, 否则显示难看甚至匹配失败。</p>
 */
public final class AnswerFormatter {

    /**
     * OCS 多答案分隔符
     */
    public static final String SEPARATOR = "#";

    private static final Pattern FENCE = Pattern.compile("```[\\w-]*[ \\t]*\\R?");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]*)`");
    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern BR = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern QUOTE = Pattern.compile("(?m)^[ \\t]*>[ \\t]?");
    private static final Pattern HEADING = Pattern.compile("(?m)^[ \\t]{0,3}#{1,6}(?:[ \\t]+|$)");
    private static final Pattern HORIZONTAL_RULE = Pattern.compile("(?m)^[ \\t]*(?:-{3,}|\\*{3,}|_{3,})[ \\t]*$");
    private static final Pattern LIST_MARKER = Pattern.compile("(?m)^[ \\t]*(?:[-+*•]|\\d{1,3}[.)、．])[ \\t]+");
    private static final Pattern BOLD = Pattern.compile("\\*\\*|__|~~");
    private static final Pattern ITALIC_STAR = Pattern.compile("\\*([^*\\n]+)\\*");
    private static final Pattern ANSWER_PREFIX_LINE = Pattern.compile("(?m)^[ \\t]*(?:正确答案|答案|答)[ \\t]*[:：][ \\t]*");
    private static final Pattern ANSWER_VERB_PREFIX =
            Pattern.compile("^(?:正确答案|答案|答)[ \\t]*(?:[:：]|是|为)[ \\t]*[:：]?[ \\t]*");
    private static final Pattern WRAPPING_QUOTES = Pattern.compile("^[\"'“”‘’]+|[\"'“”‘’]+$");
    private static final Pattern STANDALONE_LETTER = Pattern.compile("(?<![A-Za-z])([A-Za-z])(?![A-Za-z])");
    private static final Pattern COMPACT_LETTERS = Pattern.compile("(?<![A-Za-z])([A-Za-z]{2,})(?![A-Za-z])");
    private static final Pattern EXPLICIT_LETTER = Pattern.compile(
            "(?i)(?:正确答案|答案|选|应选|选择|应为|正确选项)\\s*[:：是为]?\\s*[（(【\\[]?\\s*([A-Za-z])(?![A-Za-z])");
    private static final Pattern OPTION_MARKER = Pattern.compile("^[ \\t]*([A-Za-z])[.、)．）:：][ \\t]*");
    private static final Pattern OCS_OTHER_SEPARATOR = Pattern.compile("\\s*(?:===|---|###|\\||;|；)\\s*");
    private static final Pattern NUMBERING = Pattern.compile("^(?:[-+*•]|\\d{1,3}[.)、．])[ \\t]+");
    private static final Pattern BLANK_LABEL =
            Pattern.compile("^(?:第?[一二三四五六七八九十\\d]+空|空\\d+)[ \\t]*[:：][ \\t]*");
    private static final Pattern LABEL_LINE = Pattern.compile("^(?:正确答案|答案|解析|答案解析|答)[ \\t]*[:：]?$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern MATCH_NOISE = Pattern.compile("[\\s\\p{Punct}，。、；：！？（）【】《》\"“”‘’]");
    private static final Pattern ENGLISH_NEGATIVE = Pattern.compile("(?<![a-z])(?:false|no|f)(?![a-z])");
    private static final Pattern ENGLISH_POSITIVE = Pattern.compile("(?<![a-z])(?:true|yes|t|y)(?![a-z])");
    private static final Pattern SEGMENT_SPLIT = Pattern.compile("[、,，;；/\\s]+");

    /**
     * 否定词必须优先匹配, 否则"不正确"会命中"正确"; 只放多字词, 单字词走精确匹配
     */
    private static final List<String> NEGATIVE_PHRASES = List.of("不正确", "不对", "不是", "错误", "错的");
    private static final List<String> POSITIVE_PHRASES = List.of("正确", "对的", "是的", "确定");

    private AnswerFormatter() {
    }

    /**
     * 按题型清洗答案
     *
     * @param type      归一化后的题型: single/multiple/judgement/completion/other
     * @param rawAnswer 模型原始输出
     * @param options   题目选项(可能为空), 用于校验字母或把选项文本映射回字母
     */
    public static String format(String type, String rawAnswer, String options) {
        if (rawAnswer == null || rawAnswer.isBlank()) {
            return "";
        }
        String text = stripMarkdown(rawAnswer);
        String safeType = type == null ? "other" : type;
        return switch (safeType) {
            case "single" -> formatChoice(text, options, false);
            case "multiple" -> formatChoice(text, options, true);
            case "judgement" -> formatJudgement(text);
            case "completion" -> formatCompletion(text);
            default -> sanitizeLine(text);
        };
    }

    /**
     * 去掉 Markdown 标记、"答案:"类前缀与包裹引号
     */
    private static String stripMarkdown(String raw) {
        String text = raw.replace('　', ' ');
        text = FENCE.matcher(text).replaceAll("");
        text = INLINE_CODE.matcher(text).replaceAll("$1");
        text = IMAGE.matcher(text).replaceAll("");
        text = LINK.matcher(text).replaceAll("$1");
        text = BR.matcher(text).replaceAll(" ");
        text = HTML_TAG.matcher(text).replaceAll("");
        // 引用先于标题处理, 避免 "> # 答案" 残留标题符号
        text = QUOTE.matcher(text).replaceAll("");
        text = HEADING.matcher(text).replaceAll("");
        text = HORIZONTAL_RULE.matcher(text).replaceAll("");
        text = LIST_MARKER.matcher(text).replaceAll("");
        text = BOLD.matcher(text).replaceAll("");
        text = ITALIC_STAR.matcher(text).replaceAll("$1");
        text = ANSWER_PREFIX_LINE.matcher(text).replaceAll("");
        text = ANSWER_VERB_PREFIX.matcher(text).replaceAll("");
        text = WRAPPING_QUOTES.matcher(text.trim()).replaceAll("");
        return text.trim();
    }

    /**
     * 单选/多选: 提取选项字母, 多选用 # 连接
     */
    private static String formatChoice(String text, String options, boolean multiple) {
        String cleaned = sanitizeLine(text);
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        Set<String> valid = validLetters(options);

        if (!multiple) {
            String explicit = findExplicitLetter(cleaned, valid);
            if (explicit != null) {
                return explicit;
            }
        }

        List<String> letters = extractLetters(cleaned, valid);
        if (!letters.isEmpty()) {
            if (!multiple) {
                return letters.get(0);
            }
            return String.join(SEPARATOR, new TreeSet<>(letters));
        }

        List<String> mapped = matchOptionTexts(cleaned, options, multiple);
        if (!mapped.isEmpty()) {
            if (!multiple) {
                return mapped.get(0);
            }
            return String.join(SEPARATOR, new TreeSet<>(mapped));
        }
        return cleaned;
    }

    /**
     * 判断题: 统一成 正确/错误
     */
    private static String formatJudgement(String text) {
        String cleaned = sanitizeLine(text);
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        String normalized = MATCH_NOISE.matcher(cleaned).replaceAll("").toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return cleaned;
        }
        // 不确定类回答不猜, 交给 OCS 显示原文
        if (normalized.contains("不确定") || normalized.contains("无法确定") || normalized.contains("无法判断")) {
            return cleaned;
        }
        switch (normalized) {
            case "√", "✓", "✔", "t", "true", "yes", "y", "1", "是", "对" -> {
                return "正确";
            }
            case "×", "✗", "✘", "x", "f", "false", "no", "n", "0", "非", "否", "错" -> {
                return "错误";
            }
            default -> {
                // 继续按短语匹配
            }
        }
        for (String phrase : NEGATIVE_PHRASES) {
            if (normalized.contains(phrase)) {
                return "错误";
            }
        }
        // "不错"在口语里表示正确
        if (normalized.equals("不错")) {
            return "正确";
        }
        for (String phrase : POSITIVE_PHRASES) {
            if (normalized.contains(phrase)) {
                return "正确";
            }
        }
        String lower = cleaned.toLowerCase(Locale.ROOT);
        if (ENGLISH_NEGATIVE.matcher(lower).find()) {
            return "错误";
        }
        if (ENGLISH_POSITIVE.matcher(lower).find()) {
            return "正确";
        }
        return cleaned;
    }

    /**
     * 填空: 去 Markdown 与编号, 多个空用 # 分隔; 其他题型只压成单行
     */
    private static String formatCompletion(String text) {
        // OCS 会把 ; ； | 等也当作分隔符, 换成中文逗号避免误切分
        String cleaned = OCS_OTHER_SEPARATOR.matcher(text).replaceAll("，");
        List<String> parts = new ArrayList<>();
        for (String line : cleaned.split("\\R")) {
            String part = BLANK_LABEL.matcher(line.trim()).replaceAll("");
            part = NUMBERING.matcher(part).replaceAll("").trim();
            if (part.isEmpty() || LABEL_LINE.matcher(part).matches()) {
                continue;
            }
            parts.add(part);
        }
        if (parts.isEmpty()) {
            return cleaned.trim();
        }
        return String.join(SEPARATOR, parts);
    }

    private static String sanitizeLine(String text) {
        return WHITESPACE.matcher(text).replaceAll(" ").trim();
    }

    /**
     * 优先读取"答案: X"/"选 X"这类显式字母
     */
    private static String findExplicitLetter(String text, Set<String> valid) {
        String result = null;
        Matcher matcher = EXPLICIT_LETTER.matcher(text);
        while (matcher.find()) {
            String letter = matcher.group(1).toUpperCase(Locale.ROOT);
            if (isValidLetter(letter, valid)) {
                result = letter;
            }
        }
        return result;
    }

    /**
     * 提取独立字母, 再尝试展开 AC/ABC 这类连写字母
     */
    private static List<String> extractLetters(String text, Set<String> valid) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        Matcher matcher = STANDALONE_LETTER.matcher(text);
        while (matcher.find()) {
            String letter = matcher.group(1).toUpperCase(Locale.ROOT);
            if (isValidLetter(letter, valid)) {
                found.add(letter);
            }
        }
        if (found.isEmpty()) {
            Matcher compact = COMPACT_LETTERS.matcher(text);
            while (compact.find()) {
                String run = compact.group(1).toUpperCase(Locale.ROOT);
                if (run.length() > 8) {
                    continue;
                }
                boolean allValid = true;
                for (char c : run.toCharArray()) {
                    if (!isValidLetter(String.valueOf(c), valid)) {
                        allValid = false;
                        break;
                    }
                }
                if (allValid) {
                    for (char c : run.toCharArray()) {
                        found.add(String.valueOf(c));
                    }
                }
            }
        }
        return new ArrayList<>(found);
    }

    /**
     * 解析选项范围, 用于过滤错误字母; 无法解析时返回 null 表示不限制
     */
    private static Set<String> validLetters(String options) {
        if (options == null || options.isBlank()) {
            return null;
        }
        String[] lines = options.split("\\R");
        Set<String> markers = new LinkedHashSet<>();
        int count = 0;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            count++;
            Matcher matcher = OPTION_MARKER.matcher(line);
            if (matcher.find()) {
                markers.add(matcher.group(1).toUpperCase(Locale.ROOT));
            }
        }
        if (!markers.isEmpty()) {
            return markers;
        }
        Set<String> letters = new LinkedHashSet<>();
        for (int i = 0; i < Math.min(count, 26); i++) {
            letters.add(String.valueOf((char) ('A' + i)));
        }
        return letters;
    }

    private static boolean isValidLetter(String letter, Set<String> valid) {
        if (valid != null) {
            return valid.contains(letter);
        }
        // 没有选项时只认 A-H, 避免把英文单词里的字母误判成答案
        return letter.compareTo("A") >= 0 && letter.compareTo("H") <= 0;
    }

    /**
     * 答案不是字母而是选项文本时, 映射回对应字母(多选返回全部匹配项)
     */
    private static List<String> matchOptionTexts(String text, String options, boolean multiple) {
        List<String> result = new ArrayList<>();
        if (options == null || options.isBlank()) {
            return result;
        }
        String normalizedAnswer = normalizeForMatch(text);
        if (normalizedAnswer.isEmpty()) {
            return result;
        }

        List<String> segments = new ArrayList<>();
        if (multiple) {
            for (String segment : SEGMENT_SPLIT.split(text)) {
                String normalized = normalizeForMatch(segment);
                if (!normalized.isEmpty()) {
                    segments.add(normalized);
                }
            }
        }
        segments.add(normalizedAnswer);

        char nextLetter = 'A';
        for (String line : options.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String body = line.trim();
            char letter = nextLetter;
            Matcher marker = OPTION_MARKER.matcher(body);
            if (marker.find()) {
                letter = marker.group(1).toUpperCase(Locale.ROOT).charAt(0);
                body = body.substring(marker.end());
            }
            String optionText = normalizeForMatch(body);
            if (!optionText.isEmpty() && matchesAny(segments, optionText, normalizedAnswer)
                    && !result.contains(String.valueOf(letter))) {
                result.add(String.valueOf(letter));
            }
            nextLetter = (char) (letter + 1);
        }
        return result;
    }

    private static boolean matchesAny(List<String> segments, String optionText, String normalizedAnswer) {
        for (String segment : segments) {
            if (segment.equals(optionText)) {
                return true;
            }
        }
        // 只做"选项包含答案"的单向匹配, 且要求双方都有实际长度
        return normalizedAnswer.length() >= 2 && optionText.length() >= 2
                && optionText.contains(normalizedAnswer);
    }

    private static String normalizeForMatch(String text) {
        return MATCH_NOISE.matcher(text).replaceAll("").toLowerCase(Locale.ROOT);
    }

}
