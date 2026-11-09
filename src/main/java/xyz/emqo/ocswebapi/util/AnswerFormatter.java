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

    private static final Pattern FENCE = Pattern.compile("```[\\w-]*\\s*");
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]*)`");
    private static final Pattern IMAGE = Pattern.compile("!\\[[^\\]]*]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern BR = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern HEADING = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s*");
    private static final Pattern QUOTE = Pattern.compile("(?m)^\\s*>\\s?");
    private static final Pattern LIST_MARKER = Pattern.compile("(?m)^\\s*(?:[-+*]|\\d+[.)、．])\\s+");
    private static final Pattern BOLD = Pattern.compile("\\*\\*|__|~~");
    private static final Pattern ITALIC_STAR = Pattern.compile("\\*([^*\\n]+)\\*");
    private static final Pattern ITALIC_UNDERSCORE = Pattern.compile("(?<![A-Za-z0-9])_([^_\\n]+)_(?![A-Za-z0-9])");
    private static final Pattern ANSWER_PREFIX = Pattern.compile("(?m)^\\s*(?:正确答案|答案)\\s*[:：]\\s*");
    private static final Pattern ANSWER_VERB_PREFIX = Pattern.compile("^(?:正确答案|答案)(?:是|为)\\s*");
    private static final Pattern STANDALONE_LETTER = Pattern.compile("(?<![A-Za-z])([A-Ha-h])(?![A-Za-z])");
    private static final Pattern OPTION_MARKER = Pattern.compile("^\\s*([A-Ha-h])[.、)．]\\s*");
    private static final Pattern OCS_OTHER_SEPARATOR = Pattern.compile("\\s*(?:===|---|###|\\||;|；)\\s*");
    private static final Pattern NUMBERING = Pattern.compile("^(?:[-+*]|\\d+[.)、．])\\s*");
    private static final Pattern LABEL_LINE = Pattern.compile("^(?:正确答案|答案|解析|答案解析|答)\\s*[:：]?$");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern MATCH_NOISE = Pattern.compile("[\\s\\p{Punct}，。、；：！？（）【】《》\"“”‘’]");

    /**
     * 否定词必须优先匹配, 否则"不正确"会命中"正确"
     */
    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            "不正确", "不对", "不是", "错误", "错的", "false", "否", "非", "错");
    private static final List<String> POSITIVE_KEYWORDS = List.of(
            "正确", "对的", "是的", "确定", "true", "yes", "是", "对", "√");

    private AnswerFormatter() {
    }

    /**
     * 按题型清洗答案
     *
     * @param type    归一化后的题型: single/multiple/judgement/completion/other
     * @param rawAnswer 模型原始输出
     * @param options 题目选项(可能为空), 用于校验字母或把选项文本映射回字母
     */
    public static String format(String type, String rawAnswer, String options) {
        if (rawAnswer == null || rawAnswer.isBlank()) {
            return "";
        }
        String text = stripMarkdown(rawAnswer);
        return switch (type) {
            case "single" -> formatChoice(text, options, false);
            case "multiple" -> formatChoice(text, options, true);
            case "judgement" -> formatJudgement(text);
            default -> formatCompletion(text);
        };
    }

    /**
     * 去掉 Markdown 标记与"答案:"类前缀
     */
    private static String stripMarkdown(String raw) {
        String text = raw.replace('　', ' ');
        text = FENCE.matcher(text).replaceAll("");
        text = INLINE_CODE.matcher(text).replaceAll("$1");
        text = IMAGE.matcher(text).replaceAll("");
        text = LINK.matcher(text).replaceAll("$1");
        text = BR.matcher(text).replaceAll(" ");
        text = HTML_TAG.matcher(text).replaceAll("");
        text = HEADING.matcher(text).replaceAll("");
        text = QUOTE.matcher(text).replaceAll("");
        text = LIST_MARKER.matcher(text).replaceAll("");
        text = BOLD.matcher(text).replaceAll("");
        text = ITALIC_STAR.matcher(text).replaceAll("$1");
        text = ITALIC_UNDERSCORE.matcher(text).replaceAll("$1");
        text = ANSWER_PREFIX.matcher(text).replaceAll("");
        text = ANSWER_VERB_PREFIX.matcher(text).replaceAll("");
        return text.trim();
    }

    /**
     * 单选/多选: 提取选项字母, 多选用 # 连接
     */
    private static String formatChoice(String text, String options, boolean multiple) {
        String cleaned = WHITESPACE.matcher(text).replaceAll(" ").trim();
        if (cleaned.isEmpty()) {
            return cleaned;
        }

        Set<String> valid = validLetters(options);
        LinkedHashSet<String> letters = new LinkedHashSet<>();
        Matcher matcher = STANDALONE_LETTER.matcher(cleaned);
        while (matcher.find()) {
            String letter = matcher.group(1).toUpperCase(Locale.ROOT);
            if (valid == null || valid.contains(letter)) {
                letters.add(letter);
            }
        }

        List<String> result = new ArrayList<>(letters);
        if (result.isEmpty()) {
            String mapped = matchOptionText(cleaned, options);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        if (result.isEmpty()) {
            return cleaned;
        }
        if (!multiple) {
            return result.get(0);
        }
        result.sort(String::compareTo);
        return String.join(SEPARATOR, result);
    }

    /**
     * 判断题: 统一成 正确/错误
     */
    private static String formatJudgement(String text) {
        String cleaned = text.trim();
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        String normalized = MATCH_NOISE.matcher(cleaned).replaceAll("").toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return cleaned;
        }
        switch (normalized) {
            case "√", "✓", "✔" -> {
                return "正确";
            }
            case "×", "✗", "✘", "x" -> {
                return "错误";
            }
            case "t", "true", "yes", "1", "是", "对" -> {
                return "正确";
            }
            case "f", "false", "no", "0", "非", "否", "错" -> {
                return "错误";
            }
            default -> {
                // 继续按关键词匹配
            }
        }
        for (String keyword : NEGATIVE_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return "错误";
            }
        }
        for (String keyword : POSITIVE_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return "正确";
            }
        }
        return cleaned;
    }

    /**
     * 填空/其他: 去 Markdown 与编号, 多个空用 # 分隔
     */
    private static String formatCompletion(String text) {
        // OCS 会把 ; ； | 等也当作分隔符, 换成中文逗号避免误切分
        String cleaned = OCS_OTHER_SEPARATOR.matcher(text).replaceAll("，");
        List<String> parts = new ArrayList<>();
        for (String line : cleaned.split("\\r?\\n")) {
            String part = NUMBERING.matcher(line.trim()).replaceAll("").trim();
            if (part.isEmpty() || LABEL_LINE.matcher(part).matches()) {
                continue;
            }
            parts.add(part);
        }
        if (parts.isEmpty()) {
            return cleaned.trim();
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return String.join(SEPARATOR, parts);
    }

    /**
     * 解析选项范围, 用于过滤错误字母; 无法解析时返回 null 表示不限制
     */
    private static Set<String> validLetters(String options) {
        if (options == null || options.isBlank()) {
            return null;
        }
        String[] lines = options.split("\\r?\\n");
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
        for (int i = 0; i < Math.min(count, 8); i++) {
            letters.add(String.valueOf((char) ('A' + i)));
        }
        return letters;
    }

    /**
     * 答案不是字母而是选项文本时, 映射回对应字母
     */
    private static String matchOptionText(String text, String options) {
        if (options == null || options.isBlank()) {
            return null;
        }
        String normalizedAnswer = MATCH_NOISE.matcher(text).replaceAll("").toLowerCase(Locale.ROOT);
        if (normalizedAnswer.isEmpty()) {
            return null;
        }
        char nextLetter = 'A';
        for (String line : options.split("\\r?\\n")) {
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
            String optionText = MATCH_NOISE.matcher(body).replaceAll("").toLowerCase(Locale.ROOT);
            if (!optionText.isEmpty()) {
                boolean exact = optionText.equals(normalizedAnswer);
                boolean contains = optionText.length() >= 2 && normalizedAnswer.length() >= 2
                        && (optionText.contains(normalizedAnswer) || normalizedAnswer.contains(optionText));
                if (exact || contains) {
                    return String.valueOf(letter);
                }
            }
            nextLetter = (char) (letter + 1);
        }
        return null;
    }

}
