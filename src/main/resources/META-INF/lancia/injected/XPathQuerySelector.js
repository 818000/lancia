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
 ~ Derived from Puppeteer.                                                   ~
 ~ Copyright 2022 Google Inc.                                                ~
 ~ SPDX-License-Identifier: Apache-2.0                                       ~
 ~                                                                           ~
 ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~ ~
*/

/**
 * Installs XPath query selector support into the shared injected registry.
 *
 * @param {typeof globalThis} globalThis Browser global object that owns the injected registry.
 */
(function installXPathQuerySelector(globalThis) {
  const injected = globalThis.__lanciaInjected || (globalThis.__lanciaInjected = {});

  /**
   * Iterates over nodes matching an XPath selector.
   *
   * @param {Node} root Search root.
   * @param {string} selector XPath selector expression.
   * @param {number} [maxResults=-1] Maximum number of results, or -1 for all results.
   * @yields {Node} Matching node.
   */
  function* xpathQuerySelectorAll(root, selector, maxResults = -1) {
    const doc = root.ownerDocument || document;
    const iterator = doc.evaluate(selector, root, null, XPathResult.ORDERED_NODE_ITERATOR_TYPE);
    const items = [];
    let item;

    while ((item = iterator.iterateNext())) {
      items.push(item);
      if (maxResults && items.length === maxResults) {
        break;
      }
    }

    for (let i = 0; i < items.length; i++) {
      item = items[i];
      yield item;
      items[i] = null;
    }
  }

  injected.xpathQuerySelectorAll = xpathQuerySelectorAll;
})(globalThis);
