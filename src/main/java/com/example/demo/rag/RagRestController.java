package com.example.demo.rag;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/rag")
public class RagRestController {

	private final RagService ragService;

	public RagRestController(RagService ragService) {
		this.ragService = ragService;
	}

	@PostMapping(path = "/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public RagUploadResponseDto uploadFiles(@RequestParam("files") List<MultipartFile> files) {
		return ragService.ingestFiles(files);
	}
}
