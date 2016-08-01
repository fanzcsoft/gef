/*******************************************************************************
 * Copyright (c) 2009, 2016 Fabian Steeg and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Fabian Steeg    - initial API and implementation (bug #277380)
 *     Tamas Miklossy  - usage of platform specific line separators (bug #490118)
 *******************************************************************************/

package org.eclipse.gef.dot.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Static helper methods for working with files.
 * 
 * @author Fabian Steeg (fsteeg)
 */
public final class DotFileUtils {

	private DotFileUtils() {
		/* Enforce non-instantiability */
	}

	/**
	 * @param url
	 *            The URL to resolve (can be workspace-relative)
	 * @return The file corresponding to the given URL
	 */
	public static File resolve(final URL url) {
		File resultFile = null;
		URL resolved = url;
		/*
		 * If we don't check the protocol here, the FileLocator throws a
		 * NullPointerException if the URL is a normal file URL.
		 */
		if (!url.getProtocol().equals("file")) { //$NON-NLS-1$
			throw new IllegalArgumentException("Unsupported protocol: " //$NON-NLS-1$
					+ url.getProtocol());
		}
		try {
			resultFile = new File(resolved.toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return resultFile;
	}

	/**
	 * @param text
	 *            The string to write out to a temp file
	 * @return The temp file containing the given string
	 */
	public static File write(final String text) {
		try {
			return write(text, File.createTempFile("tmp", ".dot")); //$NON-NLS-1$//$NON-NLS-2$
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param text
	 *            The string to write out to a file
	 * @param destination
	 *            The file to write the string to
	 * @return The file containing the given string
	 */
	public static File write(final String text, final File destination) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(destination), "UTF-8"));
			writer.write(text);
			writer.flush();
			writer.close();
			return destination;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param file
	 *            The file to read into a string
	 * @return The string containing the contents of the given file
	 */
	public static String read(final File file) {
		try {
			return read(new FileInputStream(file));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * Reads a string from the given input stream.
	 * 
	 * @param is
	 *            The input stream to read.
	 * @return The contents of the input stream as a {@link String}
	 * @throws IOException
	 *             In case I/O exceptions occurred.
	 */
	static String read(InputStream is) throws IOException {
		String lineSeparator = System.lineSeparator();
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(is, "UTF-8"));
		String line = reader.readLine();
		while (line != null) {
			builder.append(line).append(lineSeparator);
			line = reader.readLine();
		}
		reader.close();
		return builder.toString();
	}

	/**
	 * @param closeable
	 *            The closable to safely close
	 */
	public static void close(final Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Recursively copies the contents of the source folder to the destination
	 * folder.
	 * 
	 * @param sourceRootFolder
	 *            The source root folder
	 * @param destinationRootFolder
	 *            The destination root folder
	 */
	public static void copyAllFiles(final File sourceRootFolder,
			final File destinationRootFolder) {
		for (String name : sourceRootFolder.list()) {
			File source = new File(sourceRootFolder, name);
			if (source.isDirectory()) {
				// Recursively create sub-directories:
				File destinationFolder = new File(destinationRootFolder,
						source.getName());
				if (!destinationFolder.mkdirs()
						&& !destinationFolder.exists()) {
					throw new IllegalStateException("Could not create" + ": " //$NON-NLS-1$ //$NON-NLS-2$
							+ destinationFolder);
				}
				copyAllFiles(source, destinationFolder);
			} else {
				// Copy individual files:
				copySingleFile(destinationRootFolder, name, source);
			}
		}
	}

	/**
	 * @param destinationFolder
	 *            The destination folder
	 * @param newFileName
	 *            The name for the new file
	 * @param sourceFile
	 *            The source file to be copied into a new file in the
	 *            destination folder, with the specified name
	 * @return The newly created copy of the source file
	 */
	public static File copySingleFile(final File destinationFolder,
			final String newFileName, final File sourceFile) {
		File destinationFile = new File(destinationFolder, newFileName);
		InputStream sourceStream = null;
		FileOutputStream destinationStream = null;
		try {
			sourceStream = sourceFile.toURI().toURL().openStream();
			destinationStream = new FileOutputStream(destinationFile);
			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = sourceStream.read(buffer)) != -1) {
				destinationStream.write(buffer, 0, bytesRead);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			close(sourceStream);
			close(destinationStream);
		}
		return destinationFile;
	}
}