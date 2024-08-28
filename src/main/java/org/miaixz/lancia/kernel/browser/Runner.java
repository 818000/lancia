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
package org.miaixz.lancia.kernel.browser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.exception.LaunchException;
import org.miaixz.bus.core.lang.exception.TimeoutException;
import org.miaixz.bus.core.xyz.FileKit;
import org.miaixz.bus.core.xyz.IoKit;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Builder;
import org.miaixz.lancia.Emitter;
import org.miaixz.lancia.options.LaunchOptions;
import org.miaixz.lancia.socket.Connection;
import org.miaixz.lancia.socket.WebSocketTransport;
import org.miaixz.lancia.socket.factory.WebSocketTransportFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.win32.StdCallLibrary;

import io.reactivex.rxjava3.disposables.Disposable;

public class Runner extends Emitter<Runner.BroserEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);

    private static final Map<Process, String> pidMap = new HashMap<>();

    private static final Pattern WS_ENDPOINT_PATTERN = Pattern.compile("^DevTools listening on (ws://.*)$");
    private static final List<Runner> runners = new ArrayList<>();
    private static boolean isRegisterShutdownHook = false;
    private final String executablePath;
    private final List<String> processArguments;
    private final String tempDirectory;
    private final List<Disposable> disposables = new ArrayList<>();
    private Process process;
    private Connection connection;
    private boolean closed;

    public Runner(String executablePath, List<String> processArguments, String tempDirectory) {
        super();
        this.executablePath = executablePath;
        this.processArguments = processArguments;
        this.tempDirectory = tempDirectory;
        this.closed = true;
    }

    /**
     * 启动浏览器进程
     *
     * @param options 启动参数
     * @throws IOException io异常
     */
    public void start(LaunchOptions options) throws IOException, InterruptedException {
        if (process != null) {
            throw new RuntimeException("This process has previously been started.");
        }
        List<String> arguments = new ArrayList<>();
        arguments.add(executablePath);
        arguments.addAll(processArguments);
        ProcessBuilder processBuilder = new ProcessBuilder().command(arguments).redirectErrorStream(true);
        process = processBuilder.start();
        this.closed = false;
        pidMap.putIfAbsent(process, Builder.getProcessId(process));
        registerHook();
        addProcessListener(options);
    }

    /*
     * 获取chrome进程的pid，以方便在关闭浏览器时候能够完全杀死进程
     * 
     * @return
     * 
     * @throws IOException
     */
//    private void findPid() throws IOException, InterruptedException {
//
//        Process exec = null;
//        String command = "ps -ef";
//        try{
//            exec = Runtime.getRuntime().exec(command);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
//            String line;
//            String pid = "";
//            while (StringKit.isNotEmpty(line = reader.readLine())) {
//                if(!line.contains("chrome")) {
//                    continue;
//                }
//                if(line.contains("type=zygote") || !line.contains("type=gpu-process") || line.contains("type=utility") || line.contains("type=renderer") || line.contains("type=crashpad")){
//                   continue;
//                }
//                LOGGER.info("找到目标line {},{}", BrowserRunner.ahead,line);
//                String[] pidArray = line.trim().split("\\s+");
//                if(pidArray.length < 2){
//                    continue;
//                }
//                pid =  pidArray[1];
//                if (BrowserRunner.ahead){
//                    existedPidSet.add(pid);
//                    BrowserRunner.ahead = false;
//                }else {
//                    if(!existedPidSet.contains(pid)){
//                        String existedPid = pidMap.putIfAbsent(this.process, pid);
//                        if(existedPid == null){//找到一个新的pid,不再继续往下找
//                            LOGGER.info("found this process id {}", pid);
//                            break;
//                        }
//                    }
//                }
//
//
//            }
//
//            if(StringKit.isEmpty(pid) && !ahead){
//                LOGGER.warn("The id of the process could not be found.");
//            }
//        }finally {
//            destroyCmdExec(exec,command);
//        }
//
//    }
    /**
     * 注册钩子函数，程序关闭时，关闭浏览器
     */
    private void registerHook() {
        runners.add(this);
        if (isRegisterShutdownHook) {
            return;
        }
        synchronized (Runner.class) {
            if (!isRegisterShutdownHook) {
                RuntimeShutdownHookRegistry hook = new RuntimeShutdownHookRegistry();
                hook.register(new Thread(this::closeAllBrowser));
                isRegisterShutdownHook = true;
            }
        }
    }

    /**
     * 添加浏览器的一些事件监听 退出 SIGINT等
     *
     * @param options 启动参数
     */
    private void addProcessListener(LaunchOptions options) {
        this.disposables.add(Builder.fromEmitterEvent(this, BroserEvent.EXIT).subscribe((ignore -> this.kill())));
        if (options.getHandleSIGINT()) {
            this.disposables.add(Builder.fromEmitterEvent(this, BroserEvent.SIGINT).subscribe((ignore -> this.kill())));
        }
        if (options.getHandleSIGTERM()) {
            this.disposables.add(
                    Builder.fromEmitterEvent(this, BroserEvent.SIGTERM).subscribe((ignore -> this.closeAllBrowser())));
        }
        if (options.getHandleSIGHUP()) {
            this.disposables.add(
                    Builder.fromEmitterEvent(this, BroserEvent.SIGHUP).subscribe((ignore -> this.closeAllBrowser())));
        }
    }

    /**
     * kill 掉浏览器进程
     */
    public boolean kill() {
        if (this.closed) {
            return true;
        }
        try {
            String pid = pidMap.get(this.process);
            if ("-1".equals(pid) || StringKit.isEmpty(pid)) {
                LOGGER.warn("invalid pid ({}) ,kill chrome process failed", pid);
                return false;
            }
            Process exec = null;
            String command = "";
            if (Platform.isWindows()) {
                command = "cmd.exe /c taskkill /PID " + pid + " /F /T ";
                exec = Runtime.getRuntime().exec(command);
            } else if (Platform.isLinux() || Platform.isMac()) {
                command = "kill -9 " + pid;
                exec = Runtime.getRuntime().exec(new String[] { "/bin/sh", "-c", command });
            }
            try {
                if (exec != null) {
                    LOGGER.info("kill chrome process by pid,command:  {}", command);
                    return exec.waitFor(Builder.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
                }
            } finally {
                this.destroyCmdProcess(exec, command);
                if (StringKit.isNotEmpty(this.tempDirectory)) {
                    FileKit.remove(this.tempDirectory);
                }
            }
        } catch (Exception e) {
            LOGGER.error("kill chrome process error ", e);
            return false;
        }
        return false;
    }

    /**
     * 关闭cmd exec
     * 
     * @param process 进程
     * @throws InterruptedException 异常
     */
    private void destroyCmdProcess(Process process, String command) throws InterruptedException {
        if (process == null) {
            return;
        }
        boolean waitForResult = process.waitFor(Builder.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        if (process.isAlive() && !waitForResult) {
            process.destroyForcibly();
        }
        LOGGER.trace("The current command ({}) exit result : {}.", command, waitForResult);
    }

    /**
     * 使用java自带方法关闭chrome进程
     */
    public void destroyProcess() throws InterruptedException {
        process.destroy();
        if (process != null && process.isAlive() && !process.waitFor(Builder.DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
        }
    }

    /**
     * 通过命令行删除文件夹
     * 
     * @param path 删除的路径
     */
    private void deleteDir(String path) {
        if (StringKit.isEmpty(path) || "*".equals(path)) {
            return;
        }
        FileKit.remove(path);
    }

    /**
     * 连接上浏览器
     *
     * @param usePipe 是否是pipe连接
     * @param timeout 超时时间
     * @param slowMo  放慢频率
     * @param dumpio  浏览器版本
     * @return 连接对象
     * @throws InterruptedException 打断异常
     */
    public Connection setUpConnection(boolean usePipe, int timeout, int slowMo, boolean dumpio)
            throws InterruptedException {
        if (usePipe) {/* pipe connection */
            throw new LaunchException(
                    "Temporarily not supported pipe connect to chromuim.If you have a pipe connect to chromium idea,pleaze new a issue in github:https://github.com/839128/lancia/issues");
//            InputStream pipeRead = this.getProcess().getInputStream();
//            OutputStream pipeWrite = this.getProcess().getOutputStream();
//            PipeTransport transport = new PipeTransport(pipeRead, pipeWrite);
//            this.connection = new Connection("", transport, slowMo);

        } else {/* websocket connection */
            String waitForWSEndpoint = waitForWSEndpoint(timeout, dumpio);
            WebSocketTransport transport = WebSocketTransportFactory.create(waitForWSEndpoint);
            this.connection = new Connection(waitForWSEndpoint, transport, slowMo, timeout);
            LOGGER.trace("Connect to browser by websocket url: {}", waitForWSEndpoint);
        }
        return this.connection;
    }

    /**
     * waiting for browser ws url
     *
     * @param timeout 等待超时时间
     * @param dumpio  浏览器版本
     * @return ws url
     */
    private String waitForWSEndpoint(int timeout, boolean dumpio) {
        StreamReader reader = new StreamReader(timeout, dumpio, process.getInputStream());
        reader.start();
        return reader.getResult();
    }

    public Process getProcess() {
        return process;
    }

    // 系统奔溃或正常关闭时候，关闭所有打开的浏览器
    public void closeAllBrowser() {
        for (Runner runner : runners) {
            runner.closeBrowser();
        }
    }

    /**
     * 关闭浏览器
     */
    public void closeBrowser() {
        if (this.getClosed()) {
            return;
        }
        // 发送关闭指令
        if (this.connection != null && !this.connection.closed) {
            this.connection.send("Browser.close");
        }
        // 通过kill命令关闭
        this.disposables.forEach(Disposable::dispose);
        boolean killResult = this.kill();
        if (killResult) {
            this.closed = true;
            return;
        }
        // 采用java的Process类进行关闭
        try {
            this.destroyProcess();
        } catch (InterruptedException e) {
            LOGGER.error("Destroy chrome process error.", e);
        }
        this.closed = true;
    }

    public boolean getClosed() {
        return closed;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public Connection getConnection() {
        return connection;
    }

    public enum BroserEvent {
        /**
         * 浏览器启动
         */
        START("start"),
        /**
         * 浏览器关闭
         */
        EXIT("exit"), SIGINT("SIGINT"), SIGTERM("SIGTERM"), SIGHUP("SIGHUP");

        private String eventName;

        BroserEvent(String eventName) {
            this.eventName = eventName;
        }

        public String getEventName() {
            return eventName;
        }
    }

    /**
     * 注册钩子
     */
    public interface ShutdownHookRegistry {

        /**
         * 注册一个新的关闭钩子线程
         *
         * @param thread 线程信息
         */
        default void register(Thread thread) {
            Runtime.getRuntime().addShutdownHook(thread);
        }

        /**
         * 删除关闭线程
         *
         * @param thread 线程信息
         */
        default void remove(Thread thread) {
            Runtime.getRuntime().removeShutdownHook(thread);
        }
    }

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        long GetProcessId(Long hProcess);
    }

    static class StreamReader {
        private final StringBuilder ws = new StringBuilder();
        private final AtomicBoolean success = new AtomicBoolean(false);
        private final AtomicReference<String> chromeOutput = new AtomicReference<>("");

        private final int timeout;

        private final boolean dumpio;

        private final InputStream inputStream;

        private Thread readThread;

        public StreamReader(int timeout, boolean dumpio, InputStream inputStream) {
            this.timeout = timeout;
            this.dumpio = dumpio;
            this.inputStream = inputStream;
        }

        public void start() {
            readThread = new Thread(() -> {
                StringBuilder chromeOutputBuilder = new StringBuilder();
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (dumpio) {
                            System.out.println(line);
                        }
                        Matcher matcher = WS_ENDPOINT_PATTERN.matcher(line);
                        if (matcher.find()) {
                            ws.append(matcher.group(1));
                            success.set(true);
                            break;
                        }

                        if (chromeOutputBuilder.length() != 0) {
                            chromeOutputBuilder.append(System.lineSeparator());
                        }
                        chromeOutputBuilder.append(line);
                        chromeOutput.set(chromeOutputBuilder.toString());
                    }
                } catch (Exception e) {
                    LOGGER.error(
                            "Failed to launch the browser process!please see TROUBLESHOOTING: https://github.com/puppeteer/puppeteer/blob/master/docs/troubleshooting.md:",
                            e);
                } finally {
                    IoKit.closeQuietly(reader);
                }
            });

            readThread.start();
        }

        public String getResult() {
            try {
                readThread.join(timeout);
                if (!success.get()) {
                    IoKit.close(readThread);
                    throw new TimeoutException("Timed out after " + timeout
                            + " ms while trying to connect to the browser!" + "Chrome output: " + chromeOutput.get());
                }
            } catch (InterruptedException e) {
                IoKit.close(readThread);
                throw new RuntimeException("Interrupted while waiting for dev tools server.", e);
            }
            String url = ws.toString();
            if (StringKit.isEmpty(url)) {
                throw new LaunchException("Can't get WSEndpoint");
            }
            return url;
        }

    }

    /**
     * 基于运行时的关闭钩子
     */
    public static class RuntimeShutdownHookRegistry implements ShutdownHookRegistry {

    }

}
