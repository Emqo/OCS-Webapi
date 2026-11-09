/**
 * @Author: Emqo
 * @Date: 2026/11/09
 * @FileName: AnswerController
 * @Description:
 */
package xyz.emqo.ocswebapi.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.emqo.ocswebapi.dto.ApiResponse;
import xyz.emqo.ocswebapi.dto.SearchRequest;
import xyz.emqo.ocswebapi.service.AnswerService;

/**
 * 答案搜索控制器
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")  // 允许跨域
public class AnswerController {

    private final AnswerService answerService;

    /**
     * GET方式搜题 - 适配OCS
     */
    @GetMapping("/search")
    public ApiResponse searchByGet(
            @RequestParam String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String options) {

        log.info("收到GET搜题请求 - 题目: {}, 类型: {}", title, type);

        SearchRequest request = new SearchRequest();
        request.setTitle(title);
        request.setType(type);
        request.setOptions(options);

        return answerService.searchAnswer(request);
    }

    /**
     * POST方式搜题 - 适配OCS
     */
    @PostMapping("/search")
    public ApiResponse searchByPost(@RequestBody SearchRequest request) {
        log.info("收到POST搜题请求 - 题目: {}, 类型: {}", request.getTitle(), request.getType());
        return answerService.searchAnswer(request);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }

}
