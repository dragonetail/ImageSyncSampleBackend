package com.example.ImageSyncSampleBackend.storage;

public class StorageFileNotFoundException extends StorageException {
	private static final long serialVersionUID = 2106129420375579472L;

	public StorageFileNotFoundException(String message) {
        super(message);
    }

    public StorageFileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}