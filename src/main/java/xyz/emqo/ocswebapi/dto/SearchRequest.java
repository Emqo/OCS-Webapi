/**
 * @Author: Emqo
 * @Date: 2026/11/09
 * @FileName: SearchRequest
 * @Description:
 */
package xyz.emqo.ocswebapi.dto;

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
     * 兼容数字形式: 1-单选, 2-多选, 3-判断, 4-填空(其余归为其他)
     */
    private String type;

    /**
     * 题目选项，多个选项用\n分隔
     */
    private String options;

}
