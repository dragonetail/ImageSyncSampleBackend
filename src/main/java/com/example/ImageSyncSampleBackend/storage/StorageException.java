package com.example.ImageSyncSampleBackend.storage;

public class StorageException extends RuntimeException {
	private static final long serialVersionUID = 3512109160385012873L;

	public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}