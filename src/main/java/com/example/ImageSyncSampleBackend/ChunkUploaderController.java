package com.example.ImageSyncSampleBackend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ChunkUploaderController {
	private final Path rootLocation = Paths.get("chunk-upload-dir");;

	@GetMapping("/chunk")
	public String page() {
		return "chunk_upload";
	}

	/**
	 * @author van 检查文件存在与否
	 */
	@PostMapping("/chunk/checkFile")
	@ResponseBody
	public Boolean checkFile(@RequestParam(value = "md5File") String md5File) {
		Boolean exist = false;

		// 实际项目中，这个md5File唯一值，应该保存到数据库或者缓存中，通过判断唯一值存不存在，来判断文件存不存在，这里我就不演示了
		/*
		 * if(true) { exist = true; }
		 */
		return exist;
	}

	/**
	 * @author van 检查分片存不存在
	 * @throws MalformedURLException
	 */
	@PostMapping("/chunk/checkChunk")
	@ResponseBody
	public Boolean checkChunk(@RequestParam(value = "md5File") String md5File,
			@RequestParam(value = "chunk") Integer chunk) {
		Boolean exist = false;
		try {
			Path file = rootLocation.resolve(md5File + "/" + chunk + ".tmp");
			Resource resource = new UrlResource(file.toUri());
			if (resource.exists() || resource.isReadable()) {
				exist = true;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return exist;
	}

	/**
	 * @author van 修改上传
	 */
	@PostMapping("/chunk/upload")
	@ResponseBody
	public Boolean upload(@RequestParam(value = "file") MultipartFile file,
			@RequestParam(value = "md5File") String md5File,
			@RequestParam(value = "chunk", required = false) Integer chunk) { // 第几片，从0开始
		try {
			Path path = rootLocation.resolve(md5File + "/");
			Resource resource = new UrlResource(path.toUri());
			if (!resource.exists()) {// 目录不存在，创建目录
				Files.createDirectories(path);
			}
			String chunkName;
			if (chunk == null) {// 表示是小文件，还没有一片
				chunkName = "0.tmp";
			} else {
				chunkName = chunk + ".tmp";
			}

			Path filePath = path.resolve(chunkName);
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * @author van 合成分片
	 */
	@PostMapping("/chunk/merge")
	@ResponseBody
	public Boolean merge(@RequestParam(value = "chunks", required = false) Integer chunks,
			@RequestParam(value = "md5File") String md5File, @RequestParam(value = "name") String name)
			throws Exception {
		Path path = rootLocation.resolve(name);
		OutputStream fileOutputStream = Files.newOutputStream(path, StandardOpenOption.CREATE);
		try {
			byte[] buf = new byte[1024];
			for (long i = 0; i < chunks; i++) {
				String chunkFile = i + ".tmp";
				Path file = rootLocation.resolve(md5File + "/" + chunkFile);
				InputStream inputStream = Files.newInputStream(file, StandardOpenOption.READ);
				int len = 0;
				while ((len = inputStream.read(buf)) != -1) {
					fileOutputStream.write(buf, 0, len);
				}
				inputStream.close();
			}
			// 删除md5目录，及临时文件，这里代码省略
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			fileOutputStream.close();
		}
		return true;
	}
}
