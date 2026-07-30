package com.example.demo.chat;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatPageController {

	@GetMapping({ "/", "/chat" })
	public String index(Model model) {
		model.addAttribute("pageTitle", "Personal Assistant");
		return "index";
	}
}
