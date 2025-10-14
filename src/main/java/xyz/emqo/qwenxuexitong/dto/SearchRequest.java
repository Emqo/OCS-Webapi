/**
 * @Author: Emqo
 * @Date: 2025/10/14 20:19
 * @FileName: SearchRequest
 * @Description:
 */
package xyz.emqo.qwenxuexitong.dto;

import lombok.Data;

/**
 * 搜题请求
 */
@Data
public class SearchRequest {

    /**
     * 题目内容
     */
    private String title;

    /**
     * 题目类型: single(单选), multiple(多选), judgement(判断), completion(填空)
     */
    private String type;

    /**
     * 题目选项，多个选项用\n分隔
     */
    private String options;

}