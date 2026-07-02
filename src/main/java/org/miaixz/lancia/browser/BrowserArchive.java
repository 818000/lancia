/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                           ~
 ~ Copyright (c) 2015-2026 miaixz.org and other contributors.                ~
 ~                                                                           ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");           ~
 ~ you may not use this file except in compliance with the License.          ~
 ~ You may obtain a copy of the License at                                   ~
 ~                                                                           ~
 ~      https://www.apache.org/licenses/LICENSE-2.0                          ~
 ~                                                                           ~
 ~ Unless required by applicable law or agreed to in writing, software       ~
 ~ distributed under the License is distributed on an "AS IS" BASIS,         ~
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  ~
 ~ See the License for the specific language governing permissions and       ~
 ~ limitations under the License.                                            ~
 ~                                                                           ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia.browser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Charset;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.core.xyz.ThreadKit;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.runtime.ResourceLimits;

/**
 * Handles browser archive extraction and installation file cleanup.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class BrowserArchive {

    /**
     * Shared constant for command timeout.
     */
    private static final Duration COMMAND_TIMEOUT = Duration.ofMinutes(30L);
    /**
     * Maximum retries for path removal.
     */
    private static final int REMOVE_MAX_RETRIES = 5;
    /**
     * Delay between path removal retries.
     */
    private static final long REMOVE_RETRY_DELAY_MILLIS = 500L;
    /**
     * TAR block size.
     */
    private static final int TAR_BLOCK_SIZE = 512;
    /**
     * ZIP central directory header signature.
     */
    private static final int ZIP_CENTRAL_DIRECTORY_SIGNATURE = 0x02014b50;
    /**
     * ZIP end of central directory signature.
     */
    private static final int ZIP_END_OF_CENTRAL_DIRECTORY_SIGNATURE = 0x06054b50;
    /**
     * ZIP general purpose bit flag for UTF-8 file names.
     */
    private static final int ZIP_UTF8_FLAG = 1 << 11;
    /**
     * POSIX file type mask.
     */
    private static final int POSIX_FILE_TYPE_MASK = 0170000;
    /**
     * POSIX directory file type.
     */
    private static final int POSIX_DIRECTORY_TYPE = 0040000;
    /**
     * POSIX symbolic link file type.
     */
    private static final int POSIX_SYMLINK_TYPE = 0120000;

    /**
     * Creates a browser archive.
     */
    private BrowserArchive() {
        // No initialization required.
    }

    /**
     * Handles unpack archive.
     *
     * @param archivePath archive path value
     * @param folderPath  folder path value
     */
    public static void unpackArchive(Path archivePath, Path folderPath) {
        Path actualArchivePath = Assert.notNull(archivePath, "archivePath").toAbsolutePath().normalize();
        Path actualFolderPath = Assert.notNull(folderPath, "folderPath").toAbsolutePath().normalize();
        String archiveName = actualArchivePath.getFileName().toString();
        Logger.debug(
                true,
                "Browser",
                "Archive unpack started: archive={}, output={}",
                actualArchivePath,
                actualFolderPath);
        if (!archiveName.endsWith(".zip") && !archiveName.endsWith(".tar.bz2") && !archiveName.endsWith(".tar.xz")
                && !archiveName.endsWith(".tar.gz") && !archiveName.endsWith(".tgz") && !archiveName.endsWith(".dmg")
                && !archiveName.endsWith(".exe") && !archiveName.endsWith(".7z")) {
            throw new IllegalArgumentException("Invalid browser archive format: " + actualArchivePath);
        }
        if (!Files.isRegularFile(actualArchivePath)) {
            throw new IllegalArgumentException("Browser archive is not a regular file: " + actualArchivePath);
        }
        try {
            FileKit.mkdir(actualFolderPath.toFile());
            if (archiveName.endsWith(".zip")) {
                extractZip(actualArchivePath, actualFolderPath);
            } else if (archiveName.endsWith(".tar.bz2")) {
                extractTar(actualArchivePath, actualFolderPath, "bzip2");
            } else if (archiveName.endsWith(".tar.xz")) {
                extractTar(actualArchivePath, actualFolderPath, "xz");
            } else if (archiveName.endsWith(".tar.gz") || archiveName.endsWith(".tgz")) {
                extractTarGzip(actualArchivePath, actualFolderPath);
            } else if (archiveName.endsWith(".dmg")) {
                installDmg(actualArchivePath, actualFolderPath);
            } else if (archiveName.endsWith(".exe")) {
                extractWindowsFirefoxExe(actualArchivePath, actualFolderPath);
            } else {
                extract7z(actualArchivePath, actualFolderPath);
            }
            Logger.debug(
                    false,
                    "Browser",
                    "Archive unpack completed: archive={}, output={}",
                    actualArchivePath,
                    actualFolderPath);
        } catch (RuntimeException ex) {
            Logger.error(
                    false,
                    "Browser",
                    ex,
                    "Archive unpack failed: archive={}, output={}",
                    actualArchivePath,
                    actualFolderPath);
            deletePartialOutput(actualFolderPath, ex);
            throw new IllegalStateException(
                    "Failed to unpack browser archive: " + actualArchivePath + " -> " + actualFolderPath, ex);
        }
    }

    /**
     * Removes a path recursively with Puppeteer-compatible force and retry semantics.
     *
     * @param path target path
     * @return async removal completion
     */
    public static CompletableFuture<Void> rm(Path path) {
        return CompletableFuture.runAsync(() -> rmSync(path));
    }

    /**
     * Removes a path recursively with Puppeteer-compatible force and retry semantics.
     *
     * @param path target path text
     * @return async removal completion
     */
    public static CompletableFuture<Void> rm(String path) {
        return rm(StringKit.isBlank(path) ? null : Path.of(path));
    }

    /**
     * Removes a path recursively with Puppeteer-compatible force and retry semantics.
     *
     * @param path target path text
     */
    public static void rmSync(String path) {
        rmSync(StringKit.isBlank(path) ? null : Path.of(path));
    }

    /**
     * Removes a path recursively with Puppeteer-compatible force and retry semantics.
     *
     * @param path target path
     */
    public static void rmSync(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        Path actualPath = path.toAbsolutePath().normalize();
        IOException failure = null;
        for (int i = 0; i <= REMOVE_MAX_RETRIES; i++) {
            try {
                FileKit.remove(actualPath.toFile());
                if (!Files.exists(actualPath)) {
                    Logger.trace(false, "Browser", "Path removed: {}", actualPath);
                    return;
                }
            } catch (RuntimeException ex) {
                failure = new IOException("Failed to delete path: " + actualPath, ex);
            }
            if (i < REMOVE_MAX_RETRIES) {
                sleepBeforeRemoveRetry();
            }
        }
        if (failure != null) {
            Logger.warn(false, "Browser", failure, "Path removal failed after retries: {}", actualPath);
            throw new IllegalStateException("Failed to delete path: " + actualPath, failure);
        }
        Logger.warn(false, "Browser", "Path still exists after removal retries: {}", actualPath);
        throw new IllegalStateException("Path still exists after deletion: " + actualPath);
    }

    /**
     * Handles extract zip.
     *
     * @param archivePath archive path value
     * @param folderPath  folder path value
     */
    private static void extractZip(Path archivePath, Path folderPath) {
        Map<String, ZipEntryMetadata> metadata = readZipMetadata(archivePath);
        try (ZipInputStream source = new ZipInputStream(Files.newInputStream(archivePath))) {
            ZipEntry entry;
            byte[] buffer = new byte[Normal._8192];
            while ((entry = source.getNextEntry()) != null) {
                ZipEntryMetadata entryMetadata = metadata.getOrDefault(entry.getName(), ZipEntryMetadata.EMPTY);
                Path outputPath = safeOutputPath(folderPath, entry.getName());
                if (entry.isDirectory() || entryMetadata.isDirectory()) {
                    FileKit.mkdir(outputPath.toFile());
                    applyPosixPermissions(outputPath, entryMetadata.permissions());
                    source.closeEntry();
                    continue;
                }
                Path parent = outputPath.getParent();
                if (parent != null) {
                    FileKit.mkdir(parent.toFile());
                }
                if (entryMetadata.isSymlink()) {
                    Path target = safeSymlinkTarget(
                            folderPath,
                            outputPath,
                            StringKit.toString(readZipEntry(source), Charset.UTF_8));
                    Files.createSymbolicLink(outputPath, target);
                    source.closeEntry();
                    continue;
                }
                try (OutputStream target = Files.newOutputStream(outputPath)) {
                    int read;
                    while ((read = source.read(buffer)) >= 0) {
                        if (read > 0) {
                            target.write(buffer, 0, read);
                        }
                    }
                }
                applyPosixPermissions(outputPath, entryMetadata.permissions());
                source.closeEntry();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to extract ZIP archive: " + archivePath, ex);
        }
    }

    /**
     * Handles extract tar.
     *
     * @param archivePath           archive path value
     * @param folderPath            folder path value
     * @param decompressUtilityName decompress utility name value
     */
    private static void extractTar(Path archivePath, Path folderPath, String decompressUtilityName) {
        switch (decompressUtilityName) {
            case "bzip2" -> run("tar", "-xjf", archivePath.toString(), "-C", folderPath.toString());
            case "xz" -> run("tar", "-xJf", archivePath.toString(), "-C", folderPath.toString());
            default -> throw new IllegalArgumentException("Invalid tar decompress utility: " + decompressUtilityName);
        }
    }

    /**
     * Handles extract tar.gz.
     *
     * @param archivePath archive path value
     * @param folderPath  folder path value
     */
    private static void extractTarGzip(Path archivePath, Path folderPath) {
        try (InputStream source = new GZIPInputStream(Files.newInputStream(archivePath))) {
            byte[] header = new byte[TAR_BLOCK_SIZE];
            while (readTarBlock(source, header)) {
                if (isEmptyTarBlock(header)) {
                    return;
                }
                TarEntry entry = tarEntry(header);
                validateArchiveEntry(entry.name(), entry.type());
                Path outputPath = safeOutputPath(folderPath, entry.name());
                if (entry.isDirectory()) {
                    FileKit.mkdir(outputPath.toFile());
                    skipTarEntry(source, entry.size());
                    continue;
                }
                Path parent = outputPath.getParent();
                if (parent != null) {
                    FileKit.mkdir(parent.toFile());
                }
                writeTarEntry(source, outputPath, entry.size());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to extract TAR.GZ archive: " + archivePath, ex);
        }
    }

    /**
     * Handles extract7z.
     *
     * @param archivePath archive path value
     * @param folderPath  folder path value
     */
    private static void extract7z(Path archivePath, Path folderPath) {
        String output = "-o" + folderPath;
        if (tryRun("7z", "x", "-y", output, archivePath.toString())) {
            return;
        }
        run("7za", "x", "-y", output, archivePath.toString());
    }

    /**
     * Returns a safe archive output path.
     *
     * @param folderPath output folder
     * @param entryName  entry name
     * @return safe output path
     */
    private static Path safeOutputPath(Path folderPath, String entryName) {
        validateArchiveEntry(entryName, '0');
        Path actualFolderPath = folderPath.toAbsolutePath().normalize();
        Path outputPath = actualFolderPath.resolve(entryName).normalize();
        if (!outputPath.startsWith(actualFolderPath)) {
            throw new IllegalStateException("Archive entry escapes output folder: " + entryName);
        }
        return outputPath;
    }

    /**
     * Resolves and validates a ZIP symbolic link target.
     *
     * @param folderPath output folder
     * @param linkPath   symbolic link path
     * @param linkTarget target text stored in the ZIP entry
     * @return relative link target
     */
    private static Path safeSymlinkTarget(Path folderPath, Path linkPath, String linkTarget) {
        if (StringKit.isBlank(linkTarget)) {
            throw new IllegalStateException("ZIP symlink target must not be blank: " + linkPath);
        }
        Path target = Path.of(linkTarget);
        Path resolved = linkPath.getParent().resolve(target).normalize().toAbsolutePath();
        Path actualFolderPath = folderPath.toAbsolutePath().normalize();
        if (!resolved.startsWith(actualFolderPath)) {
            throw new IllegalStateException("ZIP symlink would point outside of the target directory: " + linkPath);
        }
        return target;
    }

    /**
     * Reads the remaining bytes of a ZIP entry.
     *
     * @param source ZIP stream
     * @return entry bytes
     * @throws IOException if reading fails
     */
    private static byte[] readZipEntry(InputStream source) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[Normal._8192];
        int read;
        while ((read = source.read(buffer)) >= Normal._0) {
            if (read > Normal._0) {
                output.write(buffer, Normal._0, read);
            }
        }
        return output.toByteArray();
    }

    /**
     * Reads ZIP entry metadata that is only available from the central directory.
     *
     * @param archivePath archive path
     * @return metadata by entry name
     */
    private static Map<String, ZipEntryMetadata> readZipMetadata(Path archivePath) {
        try {
            byte[] bytes = Files.readAllBytes(archivePath);
            int eocd = findZipEndOfCentralDirectory(bytes);
            if (eocd < Normal._0) {
                return Map.of();
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            long directorySize = Integer.toUnsignedLong(buffer.getInt(eocd + 12));
            long directoryOffset = Integer.toUnsignedLong(buffer.getInt(eocd + 16));
            if (directoryOffset > Integer.MAX_VALUE || directorySize > Integer.MAX_VALUE
                    || directoryOffset + directorySize > bytes.length) {
                return Map.of();
            }
            Map<String, ZipEntryMetadata> result = new HashMap<>();
            int offset = (int) directoryOffset;
            int end = (int) (directoryOffset + directorySize);
            while (offset + 46 <= end && buffer.getInt(offset) == ZIP_CENTRAL_DIRECTORY_SIGNATURE) {
                int versionMadeBy = Short.toUnsignedInt(buffer.getShort(offset + 4));
                int flags = Short.toUnsignedInt(buffer.getShort(offset + 8));
                int nameLength = Short.toUnsignedInt(buffer.getShort(offset + 28));
                int extraLength = Short.toUnsignedInt(buffer.getShort(offset + 30));
                int commentLength = Short.toUnsignedInt(buffer.getShort(offset + 32));
                int externalAttributes = buffer.getInt(offset + 38);
                int nameOffset = offset + 46;
                int next = nameOffset + nameLength + extraLength + commentLength;
                if (nameOffset + nameLength > end || next > end) {
                    break;
                }
                String name = zipEntryName(bytes, nameOffset, nameLength, flags);
                result.put(name, ZipEntryMetadata.from(versionMadeBy, externalAttributes, name));
                offset = next;
            }
            return result;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read ZIP metadata: " + archivePath, ex);
        }
    }

    /**
     * Finds the end of central directory record.
     *
     * @param bytes ZIP archive bytes
     * @return record offset, or {@code -1}
     */
    private static int findZipEndOfCentralDirectory(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int minimum = Math.max(0, bytes.length - 65_557);
        for (int offset = bytes.length - 22; offset >= minimum; offset--) {
            if (buffer.getInt(offset) == ZIP_END_OF_CENTRAL_DIRECTORY_SIGNATURE) {
                return offset;
            }
        }
        return -1;
    }

    /**
     * Decodes a ZIP entry name according to its general purpose bit flags.
     *
     * @param bytes  archive bytes
     * @param offset name offset
     * @param length name length
     * @param flags  general purpose bit flags
     * @return decoded name
     */
    private static String zipEntryName(byte[] bytes, int offset, int length, int flags) {
        if ((flags & ZIP_UTF8_FLAG) != 0) {
            return new String(bytes, offset, length, StandardCharsets.UTF_8);
        }
        return new String(bytes, offset, length, java.nio.charset.Charset.forName("CP437"));
    }

    /**
     * Applies POSIX permissions when the current file system supports them.
     *
     * @param path        extracted path
     * @param permissions POSIX permissions
     */
    private static void applyPosixPermissions(Path path, Set<PosixFilePermission> permissions) {
        if (permissions.isEmpty()) {
            return;
        }
        try {
            Files.setPosixFilePermissions(path, permissions);
        } catch (IOException | UnsupportedOperationException ignored) {
            Logger.debug(false, "Browser", "ZIP entry POSIX permissions could not be applied: {}", path);
        }
    }

    /**
     * Converts POSIX mode bits to Java permissions.
     *
     * @param mode POSIX mode
     * @return Java POSIX permissions
     */
    private static Set<PosixFilePermission> posixPermissions(int mode) {
        if (mode <= Normal._0) {
            return Set.of();
        }
        EnumSet<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        if ((mode & 0400) != 0) {
            permissions.add(PosixFilePermission.OWNER_READ);
        }
        if ((mode & 0200) != 0) {
            permissions.add(PosixFilePermission.OWNER_WRITE);
        }
        if ((mode & 0100) != 0) {
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if ((mode & 0040) != 0) {
            permissions.add(PosixFilePermission.GROUP_READ);
        }
        if ((mode & 0020) != 0) {
            permissions.add(PosixFilePermission.GROUP_WRITE);
        }
        if ((mode & 0010) != 0) {
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if ((mode & 0004) != 0) {
            permissions.add(PosixFilePermission.OTHERS_READ);
        }
        if ((mode & 0002) != 0) {
            permissions.add(PosixFilePermission.OTHERS_WRITE);
        }
        if ((mode & 0001) != 0) {
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        return permissions;
    }

    /**
     * Validates an archive entry.
     *
     * @param entryName entry name
     * @param entryType entry type
     */
    private static void validateArchiveEntry(String entryName, int entryType) {
        if (StringKit.isBlank(entryName)) {
            throw new IllegalStateException("Archive entry name must not be blank.");
        }
        if (entryName.startsWith("/") || entryName.startsWith("\\") || entryName.contains("\\")) {
            throw new IllegalStateException("Archive entry path is not relative: " + entryName);
        }
        if (entryName.length() > 1 && Character.isLetter(entryName.charAt(0)) && entryName.charAt(1) == ':') {
            throw new IllegalStateException("Archive entry contains a drive prefix: " + entryName);
        }
        for (String part : entryName.split("/")) {
            if ("..".equals(part)) {
                throw new IllegalStateException("Archive entry contains a parent segment: " + entryName);
            }
        }
        if (entryType == '1' || entryType == '2' || entryType == '3' || entryType == '4' || entryType == '6') {
            throw new IllegalStateException("Archive entry type is not supported: " + entryName);
        }
        if (entryType != 0 && entryType != '0' && entryType != '5') {
            throw new IllegalStateException("Archive entry type is not supported: " + entryName);
        }
    }

    /**
     * Reads one TAR block.
     *
     * @param source source stream
     * @param block  target block
     * @return {@code true} when a full block was read
     * @throws IOException when reading fails
     */
    private static boolean readTarBlock(InputStream source, byte[] block) throws IOException {
        int offset = Normal._0;
        while (offset < TAR_BLOCK_SIZE) {
            int read = source.read(block, offset, TAR_BLOCK_SIZE - offset);
            if (read < Normal._0) {
                if (offset == Normal._0) {
                    return false;
                }
                throw new IOException("Unexpected end of TAR header.");
            }
            offset += read;
        }
        return true;
    }

    /**
     * Returns whether a TAR block is empty.
     *
     * @param block block bytes
     * @return {@code true} when the block is empty
     */
    private static boolean isEmptyTarBlock(byte[] block) {
        for (byte value : block) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses a TAR entry header.
     *
     * @param header header bytes
     * @return TAR entry
     */
    private static TarEntry tarEntry(byte[] header) {
        String name = tarText(header, 0, 100);
        String prefix = tarText(header, 345, 155);
        if (StringKit.isNotBlank(prefix)) {
            name = prefix + "/" + name;
        }
        int type = header[156] & 0xff;
        long size = tarOctal(header, 124, 12);
        return new TarEntry(name, type, size);
    }

    /**
     * Reads a TAR text field.
     *
     * @param data   data bytes
     * @param offset offset
     * @param length length
     * @return text field
     */
    private static String tarText(byte[] data, int offset, int length) {
        int end = offset;
        int limit = offset + length;
        while (end < limit && data[end] != 0) {
            end++;
        }
        return new String(data, offset, end - offset, java.nio.charset.StandardCharsets.UTF_8).trim();
    }

    /**
     * Reads a TAR octal field.
     *
     * @param data   data bytes
     * @param offset offset
     * @param length length
     * @return octal value
     */
    private static long tarOctal(byte[] data, int offset, int length) {
        long result = Normal.LONG_ZERO;
        int limit = offset + length;
        for (int i = offset; i < limit; i++) {
            int value = data[i] & 0xff;
            if (value == 0 || value == ' ') {
                continue;
            }
            if (value < '0' || value > '7') {
                throw new IllegalStateException("Invalid TAR octal field.");
            }
            result = (result << 3) + value - '0';
        }
        return result;
    }

    /**
     * Writes a TAR file entry.
     *
     * @param source     source stream
     * @param outputPath output path
     * @param size       entry size
     * @throws IOException when writing fails
     */
    private static void writeTarEntry(InputStream source, Path outputPath, long size) throws IOException {
        long remaining = size;
        byte[] buffer = new byte[Normal._8192];
        try (OutputStream target = Files.newOutputStream(outputPath)) {
            while (remaining > Normal.LONG_ZERO) {
                int read = source.read(buffer, Normal._0, (int) Math.min(buffer.length, remaining));
                if (read < Normal._0) {
                    throw new IOException("Unexpected end of TAR entry: " + outputPath);
                }
                target.write(buffer, Normal._0, read);
                remaining -= read;
            }
        }
        skipTarPadding(source, size);
    }

    /**
     * Skips a TAR entry.
     *
     * @param source source stream
     * @param size   entry size
     * @throws IOException when skipping fails
     */
    private static void skipTarEntry(InputStream source, long size) throws IOException {
        skipFully(source, size);
        skipTarPadding(source, size);
    }

    /**
     * Skips TAR padding.
     *
     * @param source source stream
     * @param size   entry size
     * @throws IOException when skipping fails
     */
    private static void skipTarPadding(InputStream source, long size) throws IOException {
        long padding = (TAR_BLOCK_SIZE - size % TAR_BLOCK_SIZE) % TAR_BLOCK_SIZE;
        skipFully(source, padding);
    }

    /**
     * Skips a fixed number of bytes.
     *
     * @param source source stream
     * @param count  byte count
     * @throws IOException when skipping fails
     */
    private static void skipFully(InputStream source, long count) throws IOException {
        long remaining = count;
        while (remaining > Normal.LONG_ZERO) {
            long skipped = source.skip(remaining);
            if (skipped <= Normal.LONG_ZERO) {
                if (source.read() < Normal._0) {
                    throw new IOException("Unexpected end of TAR stream.");
                }
                skipped = Normal._1;
            }
            remaining -= skipped;
        }
    }

    /**
     * Deletes partial output after extraction failure.
     *
     * @param folderPath output folder
     * @param failure    extraction failure
     */
    private static void deletePartialOutput(Path folderPath, RuntimeException failure) {
        try {
            if (folderPath != null && Files.exists(folderPath)) {
                FileKit.remove(folderPath.toFile());
            }
        } catch (RuntimeException cleanup) {
            failure.addSuppressed(cleanup);
        }
    }

    /**
     * Handles install dmg.
     *
     * @param dmgPath    dmg path value
     * @param folderPath folder path value
     */
    private static void installDmg(Path dmgPath, Path folderPath) {
        CommandResult attach = runForResult("hdiutil", "attach", "-nobrowse", "-noautoopen", dmgPath.toString());
        String mountPath = findVolumePath(attach.output());
        try {
            Path app = findMountedApp(Path.of(mountPath));
            run("cp", "-R", app.toString(), folderPath.toString());
        } finally {
            tryRun("hdiutil", "detach", mountPath, "-quiet");
        }
    }

    /**
     * Handles extract windows firefox exe.
     *
     * @param archivePath archive path value
     * @param folderPath  folder path value
     */
    private static void extractWindowsFirefoxExe(Path archivePath, Path folderPath) {
        ProcessBuilder builder = new ProcessBuilder(archivePath.toString(), "/ExtractDir=" + folderPath);
        builder.environment().put("__compat_layer", "RunAsInvoker");
        run(builder);
    }

    /**
     * Returns the find volume path.
     *
     * @param output output value
     * @return find volume path value
     */
    private static String findVolumePath(String output) {
        for (String line : String.valueOf(output).split("\\R")) {
            int index = line.indexOf("/Volumes/");
            if (index >= 0) {
                return line.substring(index).trim();
            }
        }
        throw new IllegalStateException("Could not find mounted volume path in hdiutil output: " + output);
    }

    /**
     * Returns the find mounted app.
     *
     * @param mountPath mount path value
     * @return find mounted app value
     */
    private static Path findMountedApp(Path mountPath) {
        try (var stream = Files.list(mountPath)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".app")).findFirst().orElseThrow(
                    () -> new IllegalStateException("Could not find app directory in mounted volume: " + mountPath));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read mounted volume: " + mountPath, ex);
        }
    }

    /**
     * Returns the try run.
     *
     * @param command command name
     * @return {@code true} when the condition matches
     */
    private static boolean tryRun(String... command) {
        try {
            run(command);
            return true;
        } catch (RuntimeException ex) {
            Logger.warn(false, "Browser", ex, "Command execution failed: {}", List.of(command));
            return false;
        }
    }

    /**
     * Handles run.
     *
     * @param command command name
     */
    private static void run(String... command) {
        run(new ProcessBuilder(command));
    }

    /**
     * Handles run.
     *
     * @param builder builder value
     */
    private static void run(ProcessBuilder builder) {
        CommandResult result = runForResult(builder);
        if (result.exitCode() != 0) {
            throw new IllegalStateException(
                    "Command failed, exit code: " + result.exitCode() + ", output: " + result.output());
        }
    }

    /**
     * Returns the run for result.
     *
     * @param command command name
     * @return run for result value
     */
    private static CommandResult runForResult(String... command) {
        return runForResult(new ProcessBuilder(command));
    }

    /**
     * Returns the run for result.
     *
     * @param builder builder value
     * @return run for result value
     */
    private static CommandResult runForResult(ProcessBuilder builder) {
        try {
            builder.redirectErrorStream(true);
            Process process = builder.start();
            CompletableFuture<String> output = CompletableFuture
                    .supplyAsync(() -> readOutput(process.getInputStream()));
            boolean finished = process.waitFor(COMMAND_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("Command timed out: " + builder.command());
            }
            return new CommandResult(process.exitValue(), output.join());
        } catch (IOException ex) {
            throw new IllegalStateException("Command could not be started: " + builder.command(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Command execution was interrupted: " + builder.command(), ex);
        }
    }

    /**
     * Returns the read output.
     *
     * @param input input source
     * @return read output value
     */
    private static String readOutput(InputStream input) {
        int limit = (int) Math.min(Integer.MAX_VALUE, ResourceLimits.defaults().getMaxProcessOutputBytes());
        byte[] retained = new byte[Math.max(0, limit)];
        byte[] buffer = new byte[8192];
        int start = 0;
        int size = 0;
        try (InputStream source = input) {
            int read;
            while ((read = source.read(buffer)) >= 0) {
                for (int index = 0; index < read && retained.length > 0; index++) {
                    int writeIndex;
                    if (size < retained.length) {
                        writeIndex = (start + size) % retained.length;
                        size++;
                    } else {
                        writeIndex = start;
                        start = (start + 1) % retained.length;
                    }
                    retained[writeIndex] = buffer[index];
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read command output.", ex);
        }
        byte[] output = new byte[size];
        for (int index = 0; index < size; index++) {
            output[index] = retained[(start + index) % retained.length];
        }
        return new String(output, Charset.UTF_8);
    }

    /**
     * Returns the system tar command.
     *
     * @return system tar command value
     */
    private static String systemTarCommand() {
        String systemRoot = System.getenv("SystemRoot");
        if (StringKit.isBlank(systemRoot)) {
            systemRoot = System.getenv("SYSTEMROOT");
        }
        if (StringKit.isBlank(systemRoot)) {
            systemRoot = "C:\\Windows";
        }
        return Path.of(systemRoot, "System32", "tar.exe").toString();
    }

    /**
     * Returns whether windows is enabled.
     *
     * @return {@code true} when the condition matches
     */
    private static boolean isWindows() {
        return BrowserPlatform.isCurrentWindows();
    }

    /**
     * Handles sleep before remove retry.
     */
    private static void sleepBeforeRemoveRetry() {
        if (!ThreadKit.sleep(REMOVE_RETRY_DELAY_MILLIS)) {
            throw new IllegalStateException("Interrupted while waiting to retry deletion.");
        }
    }

    /**
     * Carries ZIP entry metadata from the central directory.
     *
     * @param directory   whether the entry is a directory
     * @param symlink     whether the entry is a symbolic link
     * @param permissions POSIX permissions
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private record ZipEntryMetadata(boolean directory, boolean symlink, Set<PosixFilePermission> permissions) {

        /**
         * Empty metadata.
         */
        private static final ZipEntryMetadata EMPTY = new ZipEntryMetadata(false, false, Set.of());

        /**
         * Creates metadata from ZIP central directory attributes.
         *
         * @param versionMadeBy      version-made-by field
         * @param externalAttributes external attributes
         * @param name               entry name
         * @return metadata
         */
        private static ZipEntryMetadata from(int versionMadeBy, int externalAttributes, String name) {
            int unixMode = externalAttributes >>> 16;
            int fileType = unixMode & POSIX_FILE_TYPE_MASK;
            boolean directory = fileType == POSIX_DIRECTORY_TYPE || name.endsWith("/")
                    || (versionMadeBy >> 8 == Normal._0 && externalAttributes == 0x10);
            boolean symlink = fileType == POSIX_SYMLINK_TYPE;
            int permissionsMode = unixMode == Normal._0 ? directory ? 0755 : 0644 : unixMode & 0777;
            return new ZipEntryMetadata(directory, symlink, posixPermissions(permissionsMode));
        }

        /**
         * Returns whether this entry is a directory.
         *
         * @return {@code true} when this entry is a directory
         */
        boolean isDirectory() {
            return directory;
        }

        /**
         * Returns whether this entry is a symbolic link.
         *
         * @return {@code true} when this entry is a symbolic link
         */
        boolean isSymlink() {
            return symlink;
        }
    }

    /**
     * Carries a TAR entry header.
     *
     * @param name entry name
     * @param type entry type
     * @param size entry size
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private record TarEntry(String name, int type, long size) {

        /**
         * Returns whether this entry is a directory.
         *
         * @return {@code true} when this entry is a directory
         */
        boolean isDirectory() {
            return type == '5';
        }
    }

    /**
     * Carries the CommandResult data.
     *
     * @param exitCode exit code
     * @param output   output
     *
     * @author Kimi Liu
     * @since Java 17+
     */
    private record CommandResult(int exitCode, String output) {

    }

}
