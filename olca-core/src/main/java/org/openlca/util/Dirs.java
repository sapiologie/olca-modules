package org.openlca.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A simple utility class for directory operations.
 */
public final class Dirs {

	/**
	 * Returns true if and only if the given file is an empty folder.
	 */
	public static boolean isEmpty(File file) {
		if (file == null)
			return false;
		return isEmpty(file.toPath());
	}

	/**
	 * Returns true if and only if the given path points to an empty folder.
	 */
	public static boolean isEmpty(Path path) {
		if (path == null)
			return false;
		if (!Files.isDirectory(path) || !Files.exists(path))
			return false;
		try (var stream = Files.newDirectoryStream(path)) {
			return !stream.iterator().hasNext();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates a directory if it not yet exists.
	 */
	public static void make(String path) {
		make(Paths.get(path));
	}

	/**
	 * Creates a directory if it not yet exists.
	 */
	public static void make(Path dir) {
		if (dir == null)
			return;
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new RuntimeException("failed to create directories " + dir, e);
		}
	}

	/**
	 * Deletes the content from the given directory but not the directory
	 * itself.
	 */
	public static void clean(Path dir) {
		if (dir == null)
			return;
		try {
			Files.newDirectoryStream(dir).forEach(p -> {
				if (Files.isDirectory(p))
					delete(p);
				else {
					try {
						Files.delete(p);
					} catch (IOException e) {
						throw new RuntimeException("failed to delete " + p, e);
					}
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("failed to clean " + dir, e);
		}
	}

	/**
	 * Copies a directory recursively.
	 */
	public static void copy(Path from, Path to) {
		if (from == null || to == null || !Files.exists(from))
			return;
		try {
			Files.walkFileTree(from, new Copy(from, to));
		} catch (IOException e) {
			throw new RuntimeException("failed to copy " + from + " to " + to, e);
		}
	}

	public static void delete(File dir) {
		if (dir == null || !dir.exists())
			return;
		delete(dir.toPath());
	}

	/**
	 * Deletes a directory recursively.
	 */
	public static void delete(String path) {
		delete(Paths.get(path));
	}

	/**
	 * Deletes a directory recursively.
	 */
	public static void delete(Path dir) {
		if (dir == null || !Files.exists(dir))
			return;
		try {
			Files.walkFileTree(dir, new Delete());
		} catch (IOException e) {
			throw new RuntimeException("failed to delete " + dir, e);
		}
	}

	/**
	 * Moves the given directory to the new location.
	 */
	public static void move(Path from, Path to) {
		copy(from, to);
		delete(from);
	}

	/**
	 * Determines the size of the content of a directory recursively.
	 */
	public static long size(Path dir) {
		if (dir == null || !Files.exists(dir))
			return 0;
		try {
			Size size = new Size();
			Files.walkFileTree(dir, size);
			return size.size;
		} catch (IOException e) {
			throw new RuntimeException("failed to determine size of directory " + dir, e);
		}
	}

	private static class Size extends SimpleFileVisitor<Path> {

		private long size;

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
			size += attrs.size();
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path file, IOException exc) {
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
			return FileVisitResult.CONTINUE;
		}

	}

	private static class Copy extends SimpleFileVisitor<Path> {

		private final Path from;
		private final Path to;

		public Copy(Path from, Path to) {
			this.from = from;
			this.to = to;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path fromDir,
				BasicFileAttributes attrs) throws IOException {
			Path toDir = to.resolve(from.relativize(fromDir));
			if (!Files.exists(toDir))
				Files.createDirectory(toDir);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			Files.copy(file, to.resolve(from.relativize(file)),
					StandardCopyOption.REPLACE_EXISTING);
			return FileVisitResult.CONTINUE;
		}
	}

	private static class Delete extends SimpleFileVisitor<Path> {
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes atts)
				throws IOException {
			try {
				Files.delete(file);
			} catch (AccessDeniedException e) {
				// files on windows can be marked as read-only
				var f = file.toFile();
				if (!f.canWrite()) {
					f.setWritable(true);
					Files.delete(file);
				}
			}
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {
			if (exc != null)
				throw exc;
			Files.delete(dir);
			return FileVisitResult.CONTINUE;
		}
	}
}
