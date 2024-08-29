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
package org.miaixz.lancia.option;

import org.miaixz.bus.core.lang.Normal;
import org.miaixz.lancia.kernel.page.PaperFormats;
import org.miaixz.lancia.option.data.PDFMargin;

import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * 生成pdf时候需要的选项
 *
 * @author Kimi Liu
 * @since Java 17+
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PDFOptions {

    /**
     *
     * 缩放网页的渲染。金额必须介于 0.1 和 2 之间。
     */
    @Builder.Default
    public double scale = 1.00;
    /**
     * 是否显示页眉和页脚。
     */
    public boolean displayHeaderFooter;
    /**
     * 打印标题的 HTML 模板。应该是有效的 HTML，其中包含用于向其中注入值的以下类： date 格式的打印日期 title 文件标题 url 文档位置 pageNumber 当前页码 文档总页数 totalPages
     */
    @Builder.Default
    public String headerTemplate = Normal.EMPTY;
    /**
     * 打印页脚的 HTML 模板。对特殊类具有与 PDFOptions.headerTemplate 相同的约束和支持。
     */
    @Builder.Default
    public String footerTemplate = Normal.EMPTY;
    /**
     * 设置为 true 以打印背景图形。
     */
    public boolean printBackground;
    /**
     * 是否横向打印。
     */
    public boolean landscape;

    /**
     * 要打印的纸张范围，例如 1-5, 8, 11-13。 空字符串，表示打印所有页面。
     */
    @Builder.Default
    public String pageRanges = Normal.EMPTY;
    /**
     * 如果设置，则该选项优先于 width 和 height 选项
     */
    public PaperFormats format;
    /**
     * 设置纸张宽度。你可以传入一个数字或带有单位的字符串
     */
    public String width;
    /**
     * 设置纸张的高度。你可以传入一个数字或带有单位的字符串。
     */
    public String height;
    /**
     * 使页面中声明的任何 CSS @page 大小优先于 width 或 height 或 format 选项中声明的大小。 false，它将缩放内容以适合纸张尺寸。
     */
    public boolean preferCSSPageSize;
    /**
     * 设置 PDF 页边距。
     */
    @Builder.Default
    public PDFMargin margin = new PDFMargin();
    /**
     * 文件保存的路径。 如果路径是相对路径，则相对于当前工作目录进行解析。
     */
    public String path;
    /**
     * 隐藏默认的白色背景并允许生成具有透明度的 pdf。
     */
    public boolean omitBackground;
    /**
     * （实验性）生成文档大纲。
     */
    public boolean outline;
    /**
     * 实验性）生成带标签的（可访问的）PDF。
     */
    @Builder.Default
    public boolean tagged = true;
    /**
     * 超时（以毫秒为单位）。通过 0 禁用超时。 可以使用 Page.setDefaultTimeout() 更改默认值
     */
    @Builder.Default
    public int timeout = 30000;
    /**
     * 如果为真，则等待 document.fonts.ready 解析。如果页面在后台，则可能需要使用 Page.bringToFront() 激活页面。
     */
    @Builder.Default
    public boolean waitForFonts = true;

}
