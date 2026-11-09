package xyz.emqo.ocswebapi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnswerFormatterTest {

    private static final String CHOICE_OPTIONS = "A. 甲\nB. 乙\nC. 丙\nD. 丁";

    @Test
    void singleStripsMarkdownBold() {
        assertEquals("A", AnswerFormatter.format("single", "**A**", CHOICE_OPTIONS));
    }

    @Test
    void singleStripsAnswerPrefix() {
        assertEquals("B", AnswerFormatter.format("single", "答案：B", CHOICE_OPTIONS));
        assertEquals("B", AnswerFormatter.format("single", "答案是：B", CHOICE_OPTIONS));
    }

    @Test
    void singleKeepsLetterFromOptionLine() {
        assertEquals("B", AnswerFormatter.format("single", "B. 乙", CHOICE_OPTIONS));
    }

    @Test
    void singlePrefersExplicitLetterOverEarlierLetters() {
        assertEquals("B", AnswerFormatter.format("single", "A错，选B", CHOICE_OPTIONS));
    }

    @Test
    void singleMapsOptionTextToLetter() {
        assertEquals("A", AnswerFormatter.format("single", "光合作用", "A. 光合作用\nB. 呼吸作用"));
    }

    @Test
    void singleSupportsMoreThanEightOptions() {
        String options = "A. 1\nB. 2\nC. 3\nD. 4\nE. 5\nF. 6\nG. 7\nH. 8\nI. 9";
        assertEquals("I", AnswerFormatter.format("single", "I", options));
    }

    @Test
    void singleDoesNotTreatEnglishWordAsLetter() {
        assertEquals("Apple", AnswerFormatter.format("single", "Apple", null));
    }

    @Test
    void multipleExtractsChineseCommaSeparatedLetters() {
        assertEquals("A#C", AnswerFormatter.format("multiple", "A、C", CHOICE_OPTIONS));
    }

    @Test
    void multipleExtractsMarkdownBoldLetters() {
        assertEquals("A#D", AnswerFormatter.format("multiple", "**A** 和 **D**", CHOICE_OPTIONS));
    }

    @Test
    void multipleKeepsSeparator() {
        assertEquals("A#C", AnswerFormatter.format("multiple", "A#C", CHOICE_OPTIONS));
    }

    @Test
    void multipleRemovesHeadingButKeepsSeparator() {
        assertEquals("A#C", AnswerFormatter.format("multiple", "## A#C", CHOICE_OPTIONS));
    }

    @Test
    void multipleExpandsCompactLetters() {
        assertEquals("A#C", AnswerFormatter.format("multiple", "AC", CHOICE_OPTIONS));
        assertEquals("A#B#C", AnswerFormatter.format("multiple", "ABC", CHOICE_OPTIONS));
        assertEquals("A#B", AnswerFormatter.format("multiple", "ab", CHOICE_OPTIONS));
    }

    @Test
    void multipleDoesNotExpandInvalidRuns() {
        assertEquals("USB", AnswerFormatter.format("multiple", "USB", CHOICE_OPTIONS));
    }

    @Test
    void multipleMapsAllOptionTexts() {
        assertEquals("A#B", AnswerFormatter.format("multiple", "富强、民主", "A. 富强\nB. 民主\nC. 自由"));
    }

    @Test
    void multipleFallsBackToText() {
        assertEquals("以上都不对", AnswerFormatter.format("multiple", "以上都不对", null));
    }

    @Test
    void judgementNormalizesSymbols() {
        assertEquals("正确", AnswerFormatter.format("judgement", "√", null));
        assertEquals("错误", AnswerFormatter.format("judgement", "×", null));
    }

    @Test
    void judgementMatchesNegativeBeforePositive() {
        assertEquals("错误", AnswerFormatter.format("judgement", "不正确", null));
        assertEquals("错误", AnswerFormatter.format("judgement", "不对", null));
    }

    @Test
    void judgementDoesNotMisreadCompoundWords() {
        assertEquals("正确", AnswerFormatter.format("judgement", "非常正确", null));
        assertEquals("正确", AnswerFormatter.format("judgement", "不错", null));
        assertEquals("不确定", AnswerFormatter.format("judgement", "不确定", null));
        assertEquals("untrue", AnswerFormatter.format("judgement", "untrue", null));
    }

    @Test
    void judgementNormalizesKeywords() {
        assertEquals("正确", AnswerFormatter.format("judgement", "是的", null));
        assertEquals("错误", AnswerFormatter.format("judgement", "**错误**", null));
        assertEquals("错误", AnswerFormatter.format("judgement", "答案是：错误", null));
    }

    @Test
    void completionStripsMarkdown() {
        assertEquals("光合作用", AnswerFormatter.format("completion", "**光合作用**", null));
    }

    @Test
    void completionDropsLabelLines() {
        assertEquals("5", AnswerFormatter.format("completion", "## 答案\n5", null));
        assertEquals("甲", AnswerFormatter.format("completion", "> # 答案\n甲", null));
    }

    @Test
    void completionJoinsMultipleBlanks() {
        assertEquals("甲#乙", AnswerFormatter.format("completion", "1. 甲\n2. 乙", null));
        assertEquals("甲#乙", AnswerFormatter.format("completion", "第一空：甲\n第二空：乙", null));
    }

    @Test
    void completionKeepsDecimalNumber() {
        assertEquals("3.14", AnswerFormatter.format("completion", "3.14", null));
        assertEquals("C# 语言", AnswerFormatter.format("completion", "C# 语言", null));
    }

    @Test
    void completionReplacesOtherSeparators() {
        assertEquals("甲，乙", AnswerFormatter.format("completion", "甲;乙", null));
    }

    @Test
    void completionStripsPrefixAndQuotes() {
        assertEquals("水", AnswerFormatter.format("completion", "答案是：水", null));
        assertEquals("水", AnswerFormatter.format("completion", "“水”", null));
    }

    @Test
    void otherIsSingleLine() {
        assertEquals("第一行 第二行", AnswerFormatter.format("other", "第一行\n第二行", null));
        assertEquals("答案", AnswerFormatter.format("other", "[答案](http://example.com)", null));
        assertEquals("ABC", AnswerFormatter.format("other", "`ABC`", null));
    }

    @Test
    void nullTypeDoesNotThrow() {
        assertEquals("A", AnswerFormatter.format(null, "**A**", CHOICE_OPTIONS));
    }

    @Test
    void blankAnswerReturnsEmpty() {
        assertEquals("", AnswerFormatter.format("single", "   ", null));
        assertEquals("", AnswerFormatter.format("completion", null, null));
    }

    @Test
    void resultHasNoMarkdownRemnants() {
        String[] answers = {"**A**", "## B", "`C`", "~~D~~", "[D](http://x)"};
        for (String raw : answers) {
            String formatted = AnswerFormatter.format("single", raw, CHOICE_OPTIONS);
            assertTrue(formatted.matches("[A-D]"), "unexpected: " + formatted);
        }
    }

    @Test
    void formatterIsIdempotent() {
        String[][] cases = {
                {"single", "**B**", CHOICE_OPTIONS},
                {"multiple", "A、C", CHOICE_OPTIONS},
                {"judgement", "非常正确", null},
                {"completion", "1. 甲\n2. 乙", null},
        };
        for (String[] item : cases) {
            String once = AnswerFormatter.format(item[0], item[1], item[2]);
            String twice = AnswerFormatter.format(item[0], once, item[2]);
            assertEquals(once, twice);
        }
    }

}
