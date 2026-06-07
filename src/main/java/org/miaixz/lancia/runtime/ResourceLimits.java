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
package org.miaixz.lancia.runtime;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.miaixz.bus.core.basic.normal.ErrorCode;
import org.miaixz.bus.core.lang.Normal;
import org.miaixz.bus.core.lang.Symbol;
import org.miaixz.bus.core.lang.exception.BusinessException;

/**
 * Defines runtime limits used to guard protocol and browser operations.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class ResourceLimits {

    /**
     * Shared constant for chrome arg prefix.
     */
    private static final String CHROME_ARG_PREFIX = Symbol.MINUS + Symbol.MINUS;
    /**
     * Shared constant for no sandbox arg.
     */
    private static final String NO_SANDBOX_ARG = CHROME_ARG_PREFIX + "no" + Symbol.MINUS + "sandbox";
    /**
     * Shared constant for disable setuid sandbox arg.
     */
    private static final String DISABLE_SETUID_SANDBOX_ARG = CHROME_ARG_PREFIX + "disable" + Symbol.MINUS + "setuid"
            + Symbol.MINUS + "sandbox";

    /**
     * Creates a resource limits.
     */
    public ResourceLimits() {
        // No initialization required.
    }

    /**
     * Current max protocol message bytes.
     */
    private long maxProtocolMessageBytes = Normal._20 * Normal.MEBI;
    /**
     * Current max download bytes.
     */
    private long maxDownloadBytes = Normal._512 * Normal.MEBI;
    /**
     * Current max browser metadata bytes.
     */
    private long maxMetadataBytes = 2L * Normal.MEBI;
    /**
     * Current max HTTP response body bytes.
     */
    private long maxResponseBodyBytes = 50L * Normal.MEBI;
    /**
     * Current max protocol stream bytes.
     */
    private long maxProtocolStreamBytes = 50L * Normal.MEBI;
    /**
     * Current max process output bytes.
     */
    private long maxProcessOutputBytes = Normal.MEBI;
    /**
     * Current max JSON parse depth.
     */
    private int maxJsonDepth = Normal._128;
    /**
     * Current max JSON token count.
     */
    private long maxJsonTokens = 1_000_000L;
    /**
     * Current max pages.
     */
    private int maxPages = Normal._128;
    /**
     * Current max contexts.
     */
    private int maxContexts = Normal._32;
    /**
     * Current navigation timeout.
     */
    private Duration navigationTimeout = Duration.ofSeconds(Normal._30);

    /**
     * Returns the defaults.
     *
     * @return defaults value
     */
    public static ResourceLimits defaults() {
        return new ResourceLimits();
    }

    /**
     * Validates protocol message bytes.
     *
     * @param bytes bytes
     */
    public void validateProtocolMessageBytes(long bytes) {
        if (bytes > maxProtocolMessageBytes) {
            throw new BusinessException(ErrorCode._100813, "Protocol message exceeds the limit: " + bytes);
        }
    }

    /**
     * Validates download bytes.
     *
     * @param bytes bytes
     */
    public void validateDownloadBytes(long bytes) {
        if (bytes > maxDownloadBytes) {
            throw new BusinessException(ErrorCode._100813, "Download content exceeds the limit: " + bytes);
        }
    }

    /**
     * Validates browser metadata bytes.
     *
     * @param bytes bytes
     */
    public void validateMetadataBytes(long bytes) {
        if (bytes > maxMetadataBytes) {
            throw new BusinessException(ErrorCode._100813, "Browser metadata exceeds the limit: " + bytes);
        }
    }

    /**
     * Validates HTTP response body bytes.
     *
     * @param bytes bytes
     */
    public void validateResponseBodyBytes(long bytes) {
        if (bytes > maxResponseBodyBytes) {
            throw new BusinessException(ErrorCode._100813, "HTTP response body exceeds the limit: " + bytes);
        }
    }

    /**
     * Validates protocol stream bytes.
     *
     * @param bytes bytes
     */
    public void validateProtocolStreamBytes(long bytes) {
        if (bytes > maxProtocolStreamBytes) {
            throw new BusinessException(ErrorCode._100813, "Protocol stream exceeds the limit: " + bytes);
        }
    }

    /**
     * Validates process output bytes.
     *
     * @param bytes bytes
     */
    public void validateProcessOutputBytes(long bytes) {
        if (bytes > maxProcessOutputBytes) {
            throw new BusinessException(ErrorCode._100813, "Process output exceeds the limit: " + bytes);
        }
    }

    /**
     * Validates JSON parse depth.
     *
     * @param depth parse depth
     */
    public void validateJsonDepth(int depth) {
        if (depth > maxJsonDepth) {
            throw new BusinessException(ErrorCode._100813, "JSON depth exceeds the limit: " + depth);
        }
    }

    /**
     * Validates JSON token count.
     *
     * @param tokens token count
     */
    public void validateJsonTokens(long tokens) {
        if (tokens > maxJsonTokens) {
            throw new BusinessException(ErrorCode._100813, "JSON token count exceeds the limit: " + tokens);
        }
    }

    /**
     * Validates page count.
     *
     * @param pages pages
     */
    public void validatePageCount(int pages) {
        if (pages > maxPages) {
            throw new BusinessException(ErrorCode._100813, "Page count exceeds the limit: " + pages);
        }
    }

    /**
     * Validates context count.
     *
     * @param contexts contexts
     */
    public void validateContextCount(int contexts) {
        if (contexts > maxContexts) {
            throw new BusinessException(ErrorCode._100813, "Context count exceeds the limit: " + contexts);
        }
    }

    /**
     * Returns the enforce sandbox args.
     *
     * @param args arguments to pass
     * @return values
     */
    public List<String> enforceSandboxArgs(List<String> args) {
        List<String> result = new ArrayList<>();
        if (args != null) {
            for (String arg : args) {
                if (!NO_SANDBOX_ARG.equals(arg) && !DISABLE_SETUID_SANDBOX_ARG.equals(arg)) {
                    result.add(arg);
                }
            }
        }
        return List.copyOf(result);
    }

    /**
     * Returns the max protocol message bytes.
     *
     * @return max protocol message bytes
     */
    public long getMaxProtocolMessageBytes() {
        return maxProtocolMessageBytes;
    }

    /**
     * Updates max protocol message bytes.
     *
     * @param maxProtocolMessageBytes max protocol message bytes value
     */
    public void setMaxProtocolMessageBytes(long maxProtocolMessageBytes) {
        this.maxProtocolMessageBytes = nonNegative(maxProtocolMessageBytes, "maxProtocolMessageBytes");
    }

    /**
     * Returns the max download bytes.
     *
     * @return max download bytes
     */
    public long getMaxDownloadBytes() {
        return maxDownloadBytes;
    }

    /**
     * Updates max download bytes.
     *
     * @param maxDownloadBytes max download bytes value
     */
    public void setMaxDownloadBytes(long maxDownloadBytes) {
        this.maxDownloadBytes = nonNegative(maxDownloadBytes, "maxDownloadBytes");
    }

    /**
     * Returns the max metadata bytes.
     *
     * @return max metadata bytes
     */
    public long getMaxMetadataBytes() {
        return maxMetadataBytes;
    }

    /**
     * Updates max metadata bytes.
     *
     * @param maxMetadataBytes max metadata bytes value
     */
    public void setMaxMetadataBytes(long maxMetadataBytes) {
        this.maxMetadataBytes = nonNegative(maxMetadataBytes, "maxMetadataBytes");
    }

    /**
     * Returns the max response body bytes.
     *
     * @return max response body bytes
     */
    public long getMaxResponseBodyBytes() {
        return maxResponseBodyBytes;
    }

    /**
     * Updates max response body bytes.
     *
     * @param maxResponseBodyBytes max response body bytes value
     */
    public void setMaxResponseBodyBytes(long maxResponseBodyBytes) {
        this.maxResponseBodyBytes = nonNegative(maxResponseBodyBytes, "maxResponseBodyBytes");
    }

    /**
     * Returns the max protocol stream bytes.
     *
     * @return max protocol stream bytes
     */
    public long getMaxProtocolStreamBytes() {
        return maxProtocolStreamBytes;
    }

    /**
     * Updates max protocol stream bytes.
     *
     * @param maxProtocolStreamBytes max protocol stream bytes value
     */
    public void setMaxProtocolStreamBytes(long maxProtocolStreamBytes) {
        this.maxProtocolStreamBytes = nonNegative(maxProtocolStreamBytes, "maxProtocolStreamBytes");
    }

    /**
     * Returns the max process output bytes.
     *
     * @return max process output bytes
     */
    public long getMaxProcessOutputBytes() {
        return maxProcessOutputBytes;
    }

    /**
     * Updates max process output bytes.
     *
     * @param maxProcessOutputBytes max process output bytes value
     */
    public void setMaxProcessOutputBytes(long maxProcessOutputBytes) {
        this.maxProcessOutputBytes = nonNegative(maxProcessOutputBytes, "maxProcessOutputBytes");
    }

    /**
     * Returns the max JSON depth.
     *
     * @return max JSON depth
     */
    public int getMaxJsonDepth() {
        return maxJsonDepth;
    }

    /**
     * Updates max JSON depth.
     *
     * @param maxJsonDepth max JSON depth value
     */
    public void setMaxJsonDepth(int maxJsonDepth) {
        this.maxJsonDepth = (int) nonNegative(maxJsonDepth, "maxJsonDepth");
    }

    /**
     * Returns the max JSON tokens.
     *
     * @return max JSON tokens
     */
    public long getMaxJsonTokens() {
        return maxJsonTokens;
    }

    /**
     * Updates max JSON tokens.
     *
     * @param maxJsonTokens max JSON tokens value
     */
    public void setMaxJsonTokens(long maxJsonTokens) {
        this.maxJsonTokens = nonNegative(maxJsonTokens, "maxJsonTokens");
    }

    /**
     * Returns the max pages.
     *
     * @return max pages
     */
    public int getMaxPages() {
        return maxPages;
    }

    /**
     * Updates max pages.
     *
     * @param maxPages max pages value
     */
    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    /**
     * Returns the max contexts.
     *
     * @return max contexts
     */
    public int getMaxContexts() {
        return maxContexts;
    }

    /**
     * Updates max contexts.
     *
     * @param maxContexts max contexts value
     */
    public void setMaxContexts(int maxContexts) {
        this.maxContexts = maxContexts;
    }

    /**
     * Returns the navigation timeout.
     *
     * @return navigation timeout
     */
    public Duration getNavigationTimeout() {
        return navigationTimeout;
    }

    /**
     * Updates navigation timeout.
     *
     * @param navigationTimeout navigation timeout value
     */
    public void setNavigationTimeout(Duration navigationTimeout) {
        this.navigationTimeout = navigationTimeout;
    }

    /**
     * Returns a non-negative resource value.
     *
     * @param value resource value
     * @param name  field name
     * @return validated value
     */
    private static long nonNegative(long value, String name) {
        if (value < 0L) {
            throw new IllegalArgumentException(name + " must not be negative.");
        }
        return value;
    }

}
