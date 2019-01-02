package com.example.ImageSyncSampleBackend;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Controller
public class SuperFileUploaderController {
	private final Path rootLocation = Paths.get("super-file-upload-dir");;

	@PostMapping("/superFileUpload")
	@ResponseBody
	public SuperFileUploadResponse upload(@RequestParam(value = "uuid") String uuid,
			@RequestParam(value = "filename") String filename, @RequestParam(value = "size") Long size,
			@RequestParam(value = "md5") String md5,
			@RequestParam(value = "chunks", required = false, defaultValue = "1") Integer chunks,
			@RequestParam(value = "autoMerge", required = false, defaultValue = "true") Boolean autoMerge,
			HttpServletRequest request) {
		// TODO
		// 貌似Tomcat在处理Multipart的时候，所有的Param（包括以上参数）都是存成了临时文件（跟踪request.getParts可以看到），
		// 因此在大规模上传文件的时候，这些频繁的小文件可能会成为性能瓶颈
		// 可以改造所有的参数为JSON，然后客户端和服务器端分别再自行解包处理，Spring有可能能够自行解包
		System.out.println("Uploading...");
		if (!(request instanceof MultipartHttpServletRequest)) {
			throw new RuntimeException("系统错误，MultipartHttpServletRequest配置无效。");
		}
		MultipartHttpServletRequest mpRequest = (MultipartHttpServletRequest) request;
		SuperFileUploadResponse response = new SuperFileUploadResponse();
		response.setUuid(uuid);
		response.setFilename(filename);
		response.setChunks(chunks);

		try {
			Files.createDirectories(rootLocation);

			// 检查目标文件目前的状况
			Path targetFilePath = rootLocation.resolve(filename);
			File targetFile = targetFilePath.toFile();
			if (targetFile.exists()) {
				response.setExisted(true);
				response.setOldSize(targetFile.length());
				response.setOldMd5("MD5-ABCDEF-123456");// 应该通过数据库查询返回
			}

			Map<String, MultipartFile> fileMap = mpRequest.getFileMap();
			// 判断是否分块
			if (chunks == 1) {
				if (fileMap.size() == 1) {
					MultipartFile file = ((MultipartFile) (fileMap.values().toArray()[0]));
					try (InputStream inputStream = file.getInputStream()) {
						Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
					}

					response.setCompleted(true);
				}
				// 若没有文件分块上传，则直接返回当前目标文件情况
			} else {
				// 创建临时文件存放目录
				Path chunksTempPath = rootLocation.resolve(uuid);
				Resource resource = new UrlResource(chunksTempPath.toUri());
				if (!resource.exists()) {
					Files.createDirectories(chunksTempPath);
				}

				// 存储上传的块
				for (String chunk : fileMap.keySet()) {// should like "0" ~ "888"
					try (InputStream inputStream = fileMap.get(chunk).getInputStream()) {
						Files.copy(inputStream, chunksTempPath.resolve(chunk), StandardCopyOption.REPLACE_EXISTING);
					}
				}

				// 构造返回数据的结构
				Set<Integer> uploadedChunkSet = new HashSet<Integer>();
				Files.list(chunksTempPath).forEach(chunkPath -> {
					long length = chunkPath.toFile().length();
					int chunkNo = Integer.valueOf(chunkPath.getFileName().toString());

					UploadedChunk uploadedChunk = new UploadedChunk();
					uploadedChunk.setChunk(chunkNo);
					uploadedChunk.setSize(length);
					response.getUploadedChunks().add(uploadedChunk);

					uploadedChunkSet.add(chunkNo);
				});

				// 判断是否上传完成（仅仅判断上传块的个数）
				response.setCompleted(true);
				for (int i = 0; i < chunks; i++) {
					if (!uploadedChunkSet.contains(i)) {
						response.setCompleted(false);
						break;
					}
				}

				// 合并文件
				if (response.getCompleted() && autoMerge) {
					// Files.deleteIfExists(targetFilePath);
					OutputStream fileOutputStream = new BufferedOutputStream(
							Files.newOutputStream(targetFilePath, StandardOpenOption.CREATE));
					byte[] buf = new byte[8192];
					try {
						for (long i = 0; i < chunks; i++) {
							Path chunkFilePath = chunksTempPath.resolve(String.valueOf(i));
							InputStream inputStream = new BufferedInputStream(
									Files.newInputStream(chunkFilePath, StandardOpenOption.READ));
							int len = 0;
							while ((len = inputStream.read(buf)) != -1) {
								fileOutputStream.write(buf, 0, len);
							}
							inputStream.close();
						}
						response.setMerged(true);

						response.setSize(targetFilePath.toFile().length());
						response.setMd5("MD5-ABCDEF-123456");// 应该计算MD5并更新到数据库
						// TODO:添加到临时任务中，稍后删除临时目录
					} finally {
						fileOutputStream.close();
					}
				}
			}

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
			}
			System.out.println("Uploaded.");
			return response;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
