/*
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
 ~                                                                               ~
 ~ The MIT License (MIT)                                                         ~
 ~                                                                               ~
 ~ Copyright (c) 2015-2024 miaixz.org and other contributors.                    ~
 ~                                                                               ~
 ~ Permission is hereby granted, free of charge, to any person obtaining a copy  ~
 ~ of this software and associated documentation files (the "Software"), to deal ~
 ~ in the Software without restriction, including without limitation the rights  ~
 ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell     ~
 ~ copies of the Software, and to permit persons to whom the Software is         ~
 ~ furnished to do so, subject to the following conditions:                      ~
 ~                                                                               ~
 ~ The above copyright notice and this permission notice shall be included in    ~
 ~ all copies or substantial portions of the Software.                           ~
 ~                                                                               ~
 ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR    ~
 ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,      ~
 ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE   ~
 ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER        ~
 ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, ~
 ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN     ~
 ~ THE SOFTWARE.                                                                 ~
 ~                                                                               ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/
package org.miaixz.lancia;

import java.io.*;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.IoKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.bus.health.Platform;
import org.miaixz.bus.logger.Logger;
import org.miaixz.lancia.kernel.page.QueryHandler;
import org.miaixz.lancia.kernel.page.QuerySelector;
import org.miaixz.lancia.nimble.PageEvaluateType;
import org.miaixz.lancia.nimble.runtime.CallFrame;
import org.miaixz.lancia.nimble.runtime.ExceptionDetails;
import org.miaixz.lancia.nimble.runtime.RemoteObject;
import org.miaixz.lancia.socket.CDPSession;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.disposables.CancellableDisposable;

/**
 * 一些公共方法
 */
public class Builder {

    public static final Map<String, QueryHandler> CUSTOM_QUERY_HANDLERS = new HashMap<>();
    /**
     * 指定版本
     */
    public static final String VERSION = "127.0.6533.99";
    /**
     * 临时文件夹前缀
     */
    public static final String PROFILE_PREFIX = "lancia_dev_chrome_profile-";
    /**
     * 把产品存放到环境变量的所有可用字段
     */
    public static final String[] PRODUCT_ENV = { "PUPPETEER_PRODUCT", "java_config_puppeteer_product",
            "java_package_config_puppeteer_product" };
    /**
     * 把浏览器执行路径存放到环境变量的所有可用字段
     */
    public static final String[] EXECUTABLE_ENV = { "PUPPETEER_EXECUTABLE_PATH",
            "java_config_puppeteer_executable_path", "java_package_config_puppeteer_executable_path" };
    /**
     * 把浏览器版本存放到环境变量的字段
     */
    public static final String PUPPETEER_CHROMIUM_REVISION_ENV = "PUPPETEER_CHROMIUM_REVISION";
    /**
     * 读取流中的数据的buffer size
     */
    public static final int DEFAULT_BUFFER_SIZE = 8 * 1024;
    /**
     * 存放下载浏览器脚本的临时目录
     */
    public static final String SHELLS_PREFIX = "lancia_browser_install_shells-";
    public static final String CHROME_FOR_LINUX = "chrome-for-linux.sh";
    public static final String CHROME_FOR_WIN = "chrome-for-win.ps1";
    public static final String CHROME_FOR_MAC = "chrome-for-mac.sh";
    /**
     * 启动浏览器时，如果没有指定路径，那么会从以下路径搜索可执行的路径
     */
    public static final String[] PROBABLE_CHROME_EXECUTABLE_PATH = new String[] { "/usr/bin/chromium",
            "/usr/bin/chromium-browser", "/usr/bin/google-chrome-stable", "/usr/bin/google-chrome",
            "/Applications/Chromium.app/Contents/MacOS/Chromium",
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Google Chrome Canary.app/Contents/MacOS/Google Chrome Canary",
            "C:/Program Files (x86)/Google/Chrome/Application/chrome.exe",
            "C:/Program Files/Google/Chrome/Application/chrome.exe" };
    /**
     * 谷歌浏览器默认启动参数
     */
    public static final List<String> DEFAULT_ARGS = Collections.unmodifiableList(new ArrayList<>() {
        private static final long serialVersionUID = -1L;
        {
            addAll(Arrays.asList(
                    // 旧版
                    /*
                     * "--disable-background-networking", "--disable-background-timer-throttling", "--disable-breakpad",
                     * "--disable-browser-side-navigation", "--disable-client-side-phishing-detection",
                     * "--disable-default-apps", "--disable-dev-shm-usage", "--disable-extensions",
                     * "--disable-features=site-per-process", "--disable-hang-monitor", "--disable-popup-blocking",
                     * "--disable-prompt-on-repost", "--disable-sync", "--disable-translate",
                     * "--metrics-recording-only", "--no-first-run", "--safebrowsing-disable-auto-update",
                     * "--enable-automation", "--password-store=basic", "--use-mock-keychain"
                     */
                    // 新版
                    "--allow-pre-commit-input", "--disable-background-networking",
                    "--disable-background-timer-throttling", "--disable-backgrounding-occluded-windows",
                    "--disable-breakpad", "--disable-client-side-phishing-detection",
                    "--disable-component-extensions-with-background-pages", "--disable-component-update",
                    "--disable-default-apps", "--disable-dev-shm-usage", "--disable-extensions",
                    "--disable-hang-monitor", "--disable-infobars", "--disable-ipc-flooding-protection",
                    "--disable-popup-blocking", "--disable-prompt-on-repost", "--disable-renderer-backgrounding",
                    "--disable-search-engine-choice-screen", "--disable-sync", "--enable-automation",
                    "--export-tagged-pdf", "--generate-pdf-document-outline", "--force-color-profile=srgb",
                    "--metrics-recording-only", "--no-first-run", "--password-store=basic", "--use-mock-keychain"));
        }
    });
    public static final Set<String> SUPPORTED_METRICS = new HashSet<>() {

        private static final long serialVersionUID = -1L;
        {
            add("Timestamp");
            add("Documents");
            add("Frames");
            add("JSEventListeners");
            add("Nodes");
            add("LayoutCount");
            add("RecalcStyleCount");
            add("LayoutDuration");
            add("RecalcStyleDuration");
            add("ScriptDuration");
            add("TaskDuration");
            add("JSHeapUsedSize");
            add("JSHeapTotalSize");
        }
    };
    /**
     * fastjson的一个实例
     */
    public static final ObjectMapper OBJECTMAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    /**
     * 从浏览器的websocket接受到消息中有以下这些字段，在处理消息用到这些字段
     */
    public static final String MESSAGE_METHOD_PROPERTY = "method";
    public static final String MESSAGE_PARAMS_PROPERTY = "params";
    public static final String MESSAGE_ID_PROPERTY = "id";
    public static final String MESSAGE_RESULT_PROPERTY = "result";
    public static final String MESSAGE_SESSION_ID_PROPERTY = "sessionId";
    public static final String MESSAGE_TARGETINFO_PROPERTY = "targetInfo";
    public static final String MESSAGE_TYPE_PROPERTY = "type";
    public static final String MESSAGE_ERROR_PROPERTY = "error";
    public static final String MESSAGE_MESSAGE_PROPERTY = "message";
    public static final String MESSAGE_DATA_PROPERTY = "data";
    public static final String MESSAGE_TARGETID_PROPERTY = "targetId";
    public static final String MESSAGE_STREAM_PROPERTY = "stream";
    public static final String MESSAGE_EOF_PROPERTY = "eof";
    public static final String MESSAGE_STREAM_DATA_PROPERTY = "data";
    public static final String MESSAGE_BASE64ENCODED_PROPERTY = "base64Encoded";
    /**
     * 默认的超时时间：启动浏览器实例超时，websocket接受消息超时等
     */
    public static final int DEFAULT_TIMEOUT = 30000;
    /**
     * 追踪信息的默认分类
     */
    public static final Set<String> DEFAULTCATEGORIES = new LinkedHashSet<>() {
        private static final long serialVersionUID = -1L;
        {
            add("-*");
            add("devtools.timeline");
            add("v8.execute");
            add("disabled-by-default-devtools.timeline");
            add("disabled-by-default-devtools.timeline.frame");
            add("toplevel");
            add("blink.console");
            add("blink.user_timing");
            add("latencyInfo");
            add("disabled-by-default-devtools.timeline.stack");
            add("disabled-by-default-v8.cpu_profiler");
            add("disabled-by-default-v8.cpu_profiler.hires");
        }
    };
    public static final String LINUX = "linux64";
    public static final String MAC_ARM64 = "mac-arm64";
    public static final String MAC_X64 = "mac-x64";
    public static final String WIN32 = "win32";
    public static final String WIN64 = "win64";

    public static String createProtocolError(JsonNode node) {
        JsonNode methodNode = node.get(MESSAGE_METHOD_PROPERTY);
        JsonNode errNode = node.get(MESSAGE_MESSAGE_PROPERTY);
        JsonNode errorMsg = errNode.get(MESSAGE_MESSAGE_PROPERTY);
        String method = "";
        if (methodNode != null) {
            method = methodNode.asText();
        }
        String message = "Protocol error " + method + ": " + errorMsg;
        JsonNode dataNode = errNode.get(MESSAGE_DATA_PROPERTY);
        if (dataNode != null) {
            message += " " + dataNode;
        }
        return message;
    }

    public static String platform() {
        return System.getProperty("os.name");
    }

    public static String join(String root, String... args) {
        return Paths.get(root, args).toString();
    }

    /**
     * read stream from protocol : example for tracing file
     *
     * @param client  CDPSession
     * @param handler 发送给websocket的参数
     * @param path    文件存放的路径
     * @param isSync  是否是在新的线程中执行
     * @throws IOException 操作文件的异常
     * @return 可能是feture，可能是字节数组
     */
    public static Object readProtocolStream(CDPSession client, String handler, String path, boolean isSync)
            throws IOException {
        if (isSync) {
            return ForkJoinPool.commonPool().submit(() -> {
                try {
                    printPDF(client, handler, path);
                } catch (IOException e) {
                    Logger.error("Method readProtocolStream error", e);
                }
            });
        } else {
            return printPDF(client, handler, path);
        }
    }

    private static byte[] printPDF(CDPSession client, String handler, String path) throws IOException {
        boolean eof = false;
        File file = null;
        BufferedOutputStream writer = null;
        BufferedInputStream reader = null;

        if (StringKit.isNotEmpty(path)) {
            file = new File(path);
            FileKit.createTempFile(file.getParentFile());
        }

        Map<String, Object> params = new HashMap<>();
        params.put("handle", handler);
        try {

            if (file != null) {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                writer = new BufferedOutputStream(fileOutputStream);
            }
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            byte[] bytes;
            List<byte[]> bufs = new ArrayList<>();
            int byteLength = 0;

            while (!eof) {
                JsonNode response = client.send("IO.read", params);
                JsonNode eofNode = response.get(MESSAGE_EOF_PROPERTY);
                JsonNode base64EncodedNode = response.get(MESSAGE_BASE64ENCODED_PROPERTY);
                JsonNode dataNode = response.get(MESSAGE_STREAM_DATA_PROPERTY);
                String dataText;

                if (dataNode != null && StringKit.isNotEmpty(dataText = dataNode.asText())) {
                    try {
                        if (base64EncodedNode != null && base64EncodedNode.asBoolean()) {
                            bytes = Base64.getDecoder().decode(dataText);
                        } else {
                            bytes = dataNode.asText().getBytes();
                        }
                        bufs.add(bytes);
                        byteLength += bytes.length;
                        // 转成二进制流 io
                        if (file != null) {
                            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
                            reader = new BufferedInputStream(byteArrayInputStream);
                            int read;
                            while ((read = reader.read(buffer, 0, DEFAULT_BUFFER_SIZE)) != -1) {
                                writer.write(buffer, 0, read);
                                writer.flush();
                            }
                        }
                    } finally {
                        IoKit.closeQuietly(reader);
                    }
                }
                eof = eofNode == null || eofNode.asBoolean();
            }
            client.send("IO.close", params);
            return getBytes(bufs, byteLength);
        } finally {
            IoKit.closeQuietly(writer);
            IoKit.closeQuietly(reader);
        }
    }

    /**
     * 多个字节数组转成一个字节数组
     * 
     * @param bufs       数组集合
     * @param byteLength 数组总长度
     * @return 总数组
     */
    private static byte[] getBytes(List<byte[]> bufs, int byteLength) {
        // 返回字节数组
        byte[] resultBuf = new byte[byteLength];
        int destPos = 0;
        for (byte[] buf : bufs) {
            System.arraycopy(buf, 0, resultBuf, destPos, buf.length);
            destPos += buf.length;
        }
        return resultBuf;
    }

    public static String getExceptionMessage(ExceptionDetails exceptionDetails) {
        if (exceptionDetails.getException() != null)
            return StringKit.isNotEmpty(exceptionDetails.getException().getDescription())
                    ? exceptionDetails.getException().getDescription()
                    : (String) exceptionDetails.getException().getValue();
        String message = exceptionDetails.getText();
        StringBuilder sb = new StringBuilder(message);
        if (exceptionDetails.getStackTrace() != null) {
            for (CallFrame callframe : exceptionDetails.getStackTrace().getCallFrames()) {
                String location = callframe.getUrl() + ":" + callframe.getColumnNumber() + ":"
                        + callframe.getColumnNumber();
                String functionName = StringKit.isNotEmpty(callframe.getFunctionName()) ? callframe.getFunctionName()
                        : "<anonymous>";
                sb.append("\n    at ").append(functionName).append("(").append(location).append(")");
            }
        }
        return sb.toString();
    }

    // 定义通用方法用于创建Observable
    public static <T, EventType> io.reactivex.rxjava3.core.Observable<T> fromEmitterEvent(Emitter<EventType> emitter,
            EventType eventType) {
        return io.reactivex.rxjava3.core.Observable.create(subscriber -> {
            if (!subscriber.isDisposed()) {
                Consumer<T> listener = subscriber::onNext;
                emitter.on(eventType, listener);
                Disposable disposable = new CancellableDisposable(() -> emitter.off(eventType, listener));
                subscriber.setDisposable(disposable);
            }
        });
    }

    public static boolean isString(Object value) {
        if (value == null)
            return false;
        return value.getClass().equals(String.class);
    }

    public static boolean isNumber(String s) {
        Pattern pattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");
        Matcher matcher = pattern.matcher(s);
        return matcher.matches();
    }

    public static Object valueFromRemoteObject(RemoteObject remoteObject) {
        Assert.isTrue(StringKit.isEmpty(remoteObject.getObjectId()), "Cannot extract value when objectId is given");
        if (StringKit.isNotEmpty(remoteObject.getUnserializableValue())) {
            if ("bigint".equals(remoteObject.getType()))
                return new BigInteger(remoteObject.getUnserializableValue().replace("n", ""));
            switch (remoteObject.getUnserializableValue()) {
            case "-0":
                return -0;
            case "NaN":
                return "NaN";
            case "Infinity":
                return "Infinity";
            case "-Infinity":
                return "-Infinity";
            default:
                throw new IllegalArgumentException(
                        "Unsupported unserializable value: " + remoteObject.getUnserializableValue());
            }
        }
        return remoteObject.getValue();
    }

    public static void releaseObject(CDPSession client, RemoteObject remoteObject, boolean isBlock) {
        if (StringKit.isEmpty(remoteObject.getObjectId()))
            return;
        Map<String, Object> params = new HashMap<>();
        params.put("objectId", remoteObject.getObjectId());
        try {
            client.send("Runtime.releaseObject", params, null, isBlock);
        } catch (Exception e) {
            // Exceptions might happen in case of a page been navigated or closed.
            // 重新导航到某个网页 或者页面已经关闭
            // Swallow these since they are harmless and we don't leak anything in this case.
            // 在这种情况下不需要将这个错误在线程执行中抛出，打日志记录一下就可以了
        }
    }

    public static String evaluationString(String fun, PageEvaluateType type, Object... args) {
        if (PageEvaluateType.STRING.equals(type)) {
            Assert.isTrue(args.length == 0, "Cannot evaluate a string with arguments");
            return fun;
        }
        List<String> argsList = new ArrayList<>();
        for (Object arg : args) {
            if (arg == null) {
                argsList.add("undefined");
            } else {
                try {
                    argsList.add(OBJECTMAPPER.writeValueAsString(arg));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return MessageFormat.format("({0})({1})", fun, String.join(",", argsList));
    }

    /**
     * 判断js字符串是否是一个函数
     * 
     * @param pageFunction js字符串
     * @return true代表是js函数
     */
    public static boolean isFunction(String pageFunction) {
        pageFunction = pageFunction.trim();
        return pageFunction.startsWith("function") || pageFunction.startsWith("async") || pageFunction.contains("=>");
    }

    /**
     * 获取进程id
     *
     * @param process 进程
     * @return 进程id
     */
    public static String getProcessId(Process process) {
        long pid = -1;
        Field field;
        if (Platform.isWindows()) {
            try {
                field = process.getClass().getDeclaredField("handle");
                field.setAccessible(true);
                pid = Kernel32.INSTANCE.GetProcessId((Long) field.get(process));
            } catch (Exception e) {
                Logger.error("Failed to get processId on Windows platform.", e);
            }
        } else if (Platform.isLinux() || Platform.isAIX()) {
            try {
                String version = System.getProperty("java.version");
                double jdkversion = Double.parseDouble(version.substring(0, 3));
                Class<?> clazz;
                if (jdkversion <= 1.8) {
                    clazz = Class.forName("java.lang.UNIXProcess");
                } else {
                    clazz = Class.forName("java.lang.ProcessImpl");
                }
                field = clazz.getDeclaredField("pid");
                field.setAccessible(true);
                pid = (Integer) field.get(process);
            } catch (Throwable e) {
                Logger.error("Failed to get processId on Linux or Aix platform.", e);
            }
        }
        return String.valueOf(pid);
    }

    public static String createProtocolErrorMessage(JsonNode receivedNode) {
        String message = receivedNode.get("error").get("message").asText();
        if (receivedNode.hasNonNull("error") && receivedNode.get("error").hasNonNull("data")) {
            message += " " + receivedNode.get("error").get("data").asText();
        }
        return message;
    }

    /**
     * 根据给定的前缀创建临时文件夹
     *
     * @param prefix 临时文件夹前缀
     * @return 临时文件夹路径
     */
    public static String createProfileDir(String prefix) {
        try {
            return Files.createTempDirectory(prefix).toRealPath().toString();
        } catch (Exception e) {
            throw new RuntimeException("create temp profile dir fail:", e);
        }
    }

    /**
     * 断言路径是否是可执行的exe文件
     *
     * @param executablePath 要断言的文件
     * @return 可执行，返回true
     */
    public static boolean assertExecutable(String executablePath) {
        Path path = Paths.get(executablePath);
        return Files.isRegularFile(path) && Files.isReadable(path) && Files.isExecutable(path);
    }

    public static void registerCustomQueryHandler(String name, QueryHandler handler) {
        if (CUSTOM_QUERY_HANDLERS.containsKey(name))
            throw new RuntimeException("A custom query handler named " + name + " already exists");
        Pattern pattern = Pattern.compile("^[a-zA-Z]+$");
        Matcher isValidName = pattern.matcher(name);
        if (!isValidName.matches())
            throw new IllegalArgumentException("Custom query handler names may only contain [a-zA-Z]");

        CUSTOM_QUERY_HANDLERS.put(name, handler);
    }

    public static final void unregisterCustomQueryHandler(String name) {
        CUSTOM_QUERY_HANDLERS.remove(name);
    }

    public static Map<String, QueryHandler> customQueryHandlers() {
        return CUSTOM_QUERY_HANDLERS;
    }

    public static QuerySelector getQueryHandlerAndSelector(String selector, String defaultQueryHandler) {
        Pattern pattern = Pattern.compile("^[a-zA-Z]+\\/");
        Matcher hasCustomQueryHandler = pattern.matcher(selector);
        if (!hasCustomQueryHandler.find())
            return new QuerySelector(selector, new QueryHandler() {
                @Override
                public String queryOne() {
                    return "(element,selector) =>\n" + "      element.querySelector(selector)";
                }

                @Override
                public String queryAll() {
                    return "(element,selector) =>\n" + "      element.querySelectorAll(selector)";
                }
            });
        int index = selector.indexOf("/");
        String name = selector.substring(0, index);
        String updatedSelector = selector.substring(index + 1);
        QueryHandler queryHandler = customQueryHandlers().get(name);
        if (queryHandler == null)
            throw new RuntimeException("Query set to use " + name + ", but no query handler of that name was found");
        return new QuerySelector(updatedSelector, queryHandler);
    }

    public void clearQueryHandlers() {
        CUSTOM_QUERY_HANDLERS.clear();
    }

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        long GetProcessId(Long hProcess);
    }

}
