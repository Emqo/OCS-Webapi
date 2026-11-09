package xyz.emqo.ocswebapi.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnswerFormatterTest {

    private static final String CHOICE_OPTIONS = "A. 甲\nB. 乙\nC. 丙\nD. 丁";

    @Test
    void singleStripsMarkdownBold() {
        assertEquals("A", AnswerFormatter.format("single", "**A**", CHOICE_OPTIONS));
    }

    @Test
    void singleStripsAnswerPrefix() {
        assertEquals("B", AnswerFormatter.format("single", "答案：B", CHOICE_OPTIONS));
    }

    @Test
    void singleKeepsLetterFromOptionLine() {
        assertEquals("B", AnswerFormatter.format("single", "B. 乙", CHOICE_OPTIONS));
    }

    @Test
    void singleMapsOptionTextToLetter() {
        assertEquals("A", AnswerFormatter.format("single", "光合作用", "A. 光合作用\nB. 呼吸作用"));
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
    void judgementNormalizesKeywords() {
        assertEquals("正确", AnswerFormatter.format("judgement", "是的", null));
        assertEquals("错误", AnswerFormatter.format("judgement", "**错误**", null));
    }

    @Test
    void completionStripsMarkdown() {
        assertEquals("光合作用", AnswerFormatter.format("completion", "**光合作用**", null));
    }

    @Test
    void completionDropsLabelLines() {
        assertEquals("5", AnswerFormatter.format("completion", "## 答案\n5", null));
    }

    @Test
    void completionJoinsMultipleBlanks() {
        assertEquals("甲#乙", AnswerFormatter.format("completion", "1. 甲\n2. 乙", null));
    }

    @Test
    void completionReplacesOtherSeparators() {
        assertEquals("甲，乙", AnswerFormatter.format("completion", "甲;乙", null));
    }

    @Test
    void otherStripsLinkAndCodeMarkers() {
        assertEquals("答案", AnswerFormatter.format("other", "[答案](http://example.com)", null));
        assertEquals("ABC", AnswerFormatter.format("other", "`ABC`", null));
    }

    @Test
    void blankAnswerReturnsEmpty() {
        assertEquals("", AnswerFormatter.format("single", "   ", null));
        assertEquals("", AnswerFormatter.format("completion", null, null));
    }

}
