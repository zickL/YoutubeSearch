package org.example;

import com.google.api.services.youtube.model.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.List;

@Controller
public class SearchController {

    @Autowired
    private YoutubeService youTubeService;

    @GetMapping("/")

    public String index() {
        return "index"; // 直接返回文本内容
    }

    @GetMapping("/search")

    public String search(@RequestParam String query, Model model) {
        try {
            List<SearchResult> results = youTubeService.search(query);
            model.addAttribute("results", results); // 直接返回 JSON 格式的搜索结果
        } catch (Exception e) {
            model.addAttribute("error", "An error occurred while searching: " + e.getMessage()); // 返回错误信息
        }
        return "results";
    }
}
