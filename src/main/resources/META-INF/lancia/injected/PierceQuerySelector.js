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
 * Installs shadow-DOM piercing query support into the shared injected registry.
 *
 * @param {typeof globalThis} globalThis Browser global object that owns the injected registry.
 */
(function installPierceQuerySelector(globalThis) {
  const injected = globalThis.__lanciaInjected || (globalThis.__lanciaInjected = {});

  /**
   * Queries the first element matching a selector across open shadow roots.
   *
   * @param {Document|Element|ShadowRoot} root Search root.
   * @param {string} selector CSS selector expression.
   * @returns {Element|null} Matching element, or null when no element matches.
   */
  function pierceQuerySelector(root, selector) {
    let found = null;
    /**
     * Traverses a root and its open shadow roots until a match is found.
     *
     * @param {Element|ShadowRoot} searchRoot Current search root.
     */
    const search = searchRoot => {
      const iter = document.createTreeWalker(searchRoot, NodeFilter.SHOW_ELEMENT);
      do {
        const currentNode = iter.currentNode;
        if (currentNode.shadowRoot) {
          search(currentNode.shadowRoot);
        }
        if (currentNode instanceof ShadowRoot) {
          continue;
        }
        if (currentNode !== searchRoot && !found && currentNode.matches(selector)) {
          found = currentNode;
        }
      } while (!found && iter.nextNode());
    };
    if (root instanceof Document) {
      root = root.documentElement;
    }
    search(root);
    return found;
  }

  /**
   * Queries every element matching a selector across open shadow roots.
   *
   * @param {Document|Element|ShadowRoot} element Search root.
   * @param {string} selector CSS selector expression.
   * @returns {Element[]} Matching elements.
   */
  function pierceQuerySelectorAll(element, selector) {
    const result = [];
    /**
     * Traverses a root and collects matches from open shadow roots.
     *
     * @param {Element|ShadowRoot} root Current search root.
     */
    const collect = root => {
      const iter = document.createTreeWalker(root, NodeFilter.SHOW_ELEMENT);
      do {
        const currentNode = iter.currentNode;
        if (currentNode.shadowRoot) {
          collect(currentNode.shadowRoot);
        }
        if (currentNode instanceof ShadowRoot) {
          continue;
        }
        if (currentNode !== root && currentNode.matches(selector)) {
          result.push(currentNode);
        }
      } while (iter.nextNode());
    };
    if (element instanceof Document) {
      element = element.documentElement;
    }
    collect(element);
    return result;
  }

  injected.pierceQuerySelector = pierceQuerySelector;
  injected.pierceQuerySelectorAll = pierceQuerySelectorAll;
})(globalThis);
