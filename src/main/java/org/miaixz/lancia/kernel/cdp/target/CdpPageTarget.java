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
package org.miaixz.lancia.kernel.cdp.target;

import org.miaixz.bus.core.lang.Assert;
import org.miaixz.bus.core.lang.Optional;
import org.miaixz.bus.core.xyz.StringKit;
import org.miaixz.lancia.Page;
import org.miaixz.lancia.kernel.cdp.browser.CdpBrowserContext;
import org.miaixz.lancia.kernel.cdp.page.CdpPage;
import org.miaixz.lancia.kernel.cdp.session.TargetInfo;

/**
 * CDP page target implementation.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public class CdpPageTarget extends CdpTarget {

    /**
     * Current page.
     */
    private final Page page;

    /**
     * Creates a CDP page target.
     *
     * @param targetInfo target info
     * @param opener     opener
     */
    public CdpPageTarget(TargetInfo targetInfo, CdpTarget opener) {
        this(targetInfo, opener, new CdpPage());
    }

    /**
     * Creates a CDP page target.
     *
     * @param targetInfo target info
     * @param opener     opener
     * @param page       page instance
     */
    public CdpPageTarget(TargetInfo targetInfo, CdpTarget opener, Page page) {
        super(targetInfo, opener);
        this.page = Assert.notNull(page, "page");
        CdpPage.Internal.bindTarget(this.page, this);
    }

    /**
     * Returns the page.
     *
     * @return optional value
     */
    @Override
    public Optional<Page> page() {
        return Optional.of(page);
    }

    /**
     * Handles bind browser context.
     *
     * @param browserContext browser context value
     */
    @Override
    protected void bindBrowserContext(CdpBrowserContext browserContext) {
        super.bindBrowserContext(browserContext);
        CdpPage.Internal.bindBrowserContext(page, browserContext);
    }

    /**
     * Initializes protocol state for this object.
     */
    @Override
    protected void initialize() {
        checkIfInitialized();
    }

    /**
     * Handles check if initialized.
     */
    @Override
    protected void checkIfInitialized() {
        if (StringKit.isNotBlank(targetInfo().getUrl())) {
            completeInitialization(InitializationStatus.SUCCESS);
        }
    }

}
