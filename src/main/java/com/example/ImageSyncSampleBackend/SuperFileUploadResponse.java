package com.example.ImageSyncSampleBackend;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class SuperFileUploadResponse {
	private String uuid;
	private String filename;
	private int chunks;

	private Boolean existed = false;
	private long oldSize;
	private String oldMd5;

	private Boolean completed = false;
	private Boolean merged = false;
	private long size;
	private String md5;
	private List<UploadedChunk> uploadedChunks = new ArrayList<UploadedChunk>();
}

@Data
class UploadedChunk {
	private int chunk;
	private long size;
}