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
 * Installs text query selector support into the shared injected registry.
 *
 * @param {typeof globalThis} globalThis Browser global object that owns the injected registry.
 */
(function installTextQuerySelector(globalThis) {
  const injected = globalThis.__lanciaInjected || (globalThis.__lanciaInjected = {});
  const unsuitableNodeNames = new Set(['SCRIPT', 'STYLE']);

  /**
   * Checks whether a node can participate in text matching when TextContent is unavailable.
   *
   * @param {Node} node Candidate node.
   * @returns {boolean} True when text matching is allowed for the node.
   */
  function fallbackIsSuitableNodeForTextMatching(node) {
    return !unsuitableNodeNames.has(node.nodeName) && !(document.head && document.head.contains(node));
  }

  /**
   * Creates minimal text metadata when the TextContent module is unavailable.
   *
   * @param {Node} root Text extraction root.
   * @returns {{full: string, immediate: string[]}} Full and immediate text content.
   */
  function fallbackCreateTextContent(root) {
    return {
      full: root.textContent || '',
      immediate: [],
    };
  }

  const isSuitableNodeForTextMatching =
    injected.isSuitableNodeForTextMatching || fallbackIsSuitableNodeForTextMatching;
  const createTextContent = injected.createTextContent || fallbackCreateTextContent;

  /**
   * Iterates over elements whose rendered text content contains the selector text.
   *
   * @param {Node|ShadowRoot} root Search root.
   * @param {string} selector Text selector value.
   * @yields {Element} Matching element.
   */
  function* textQuerySelectorAll(root, selector) {
    let yielded = false;
    for (const node of root.childNodes || []) {
      if (node instanceof Element && isSuitableNodeForTextMatching(node)) {
        const matches = !node.shadowRoot
          ? textQuerySelectorAll(node, selector)
          : textQuerySelectorAll(node.shadowRoot, selector);
        for (const match of matches) {
          yield match;
          yielded = true;
        }
      }
    }
    if (yielded) {
      return;
    }

    if (root instanceof Element && isSuitableNodeForTextMatching(root)) {
      const textContent = createTextContent(root);
      if (textContent.full.includes(selector)) {
        yield root;
      }
    }
  }

  injected.textQuerySelectorAll = textQuerySelectorAll;
})(globalThis);
