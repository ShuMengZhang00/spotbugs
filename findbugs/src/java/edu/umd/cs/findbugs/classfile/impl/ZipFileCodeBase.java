/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.classfile.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import edu.umd.cs.findbugs.classfile.ICodeBaseIterator;
import edu.umd.cs.findbugs.classfile.IScannableCodeBase;
import edu.umd.cs.findbugs.classfile.ResourceNotFoundException;

/**
 * Implementation of ICodeBase to read from a zip file or jar file.
 * 
 * @author David Hovemeyer
 */
public class ZipFileCodeBase extends AbstractScannableCodeBase {
	private ZipFile zipFile;

	/**
	 * Constructor.
	 * 
	 * @param fileName filename of the zip file
	 * @throws IOException
	 */
	public ZipFileCodeBase(String fileName) throws IOException {
		this.zipFile = new ZipFile(fileName);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param file the File representing the zip file
	 */
	public ZipFileCodeBase(File file) throws IOException {
		this.zipFile = new ZipFile(file);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#openResource(java.lang.String)
	 */
	public InputStream openResource(String resourceName) throws ResourceNotFoundException, IOException {
		ZipEntry entry = zipFile.getEntry(resourceName);
		if (entry == null) {
			throw new ResourceNotFoundException(resourceName);
		}
		return zipFile.getInputStream(entry);
	}
	
	class ZipCodeBaseEntry implements ICodeBaseEntry {
		ZipEntry zipEntry;
		
		public ZipCodeBaseEntry(ZipEntry zipEntry) {
			this.zipEntry = zipEntry;
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#getResourceName()
		 */
		public String getResourceName() {
			return zipEntry.getName();
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.ICodeBaseEntry#openResource()
		 */
		public InputStream openResource() throws IOException {
			return zipFile.getInputStream(zipEntry);
		}
	}

	public ICodeBaseIterator iterator() {
		final Enumeration<? extends ZipEntry> zipEntryEnumerator = zipFile.entries();
		
		return new ICodeBaseIterator() {
			ZipCodeBaseEntry nextEntry;
			
			public boolean hasNext() {
				scanForNextEntry();
				return nextEntry != null;
			}

			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.classfile.ICodeBaseIterator#next()
			 */
			public ICodeBaseEntry next() throws InterruptedException {
				scanForNextEntry();
				if (nextEntry == null) {
					throw new NoSuchElementException();
				}
				ICodeBaseEntry result = nextEntry;
				nextEntry = null;
				return result;
			}

			private void scanForNextEntry() {
				while (nextEntry == null) {
					if (!zipEntryEnumerator.hasMoreElements()) {
						return;
					}

					ZipEntry zipEntry = zipEntryEnumerator.nextElement();
				
					if (!zipEntry.isDirectory()) {
						nextEntry = new ZipCodeBaseEntry(zipEntry);
						break;
					}
				}
			}
			
		};
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.ICodeBase#close()
	 */
	public void close() {
		try {
			zipFile.close();
		} catch (IOException e) {
			// Ignore
		}
	}
}
