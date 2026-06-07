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
 * Installs text-content extraction and cache invalidation support.
 *
 * @param {typeof globalThis} globalThis Browser global object that owns the injected registry.
 */
(function installTextContent(globalThis) {
  const injected = globalThis.__lanciaInjected || (globalThis.__lanciaInjected = {});
  const trivialValueInputTypes = new Set(['checkbox', 'image', 'radio']);
  const unsuitableNodeNames = new Set(['SCRIPT', 'STYLE']);
  const textContentCache = new WeakMap();
  const observedNodes = new WeakSet();

  /**
   * Checks whether a form control contributes a non-trivial value to text matching.
   *
   * @param {Node} node Candidate node.
   * @returns {boolean} True when the node value should be matched as text.
   */
  function isNonTrivialValueNode(node) {
    if (node instanceof HTMLSelectElement) {
      return true;
    }
    if (node instanceof HTMLTextAreaElement) {
      return true;
    }
    if (node instanceof HTMLInputElement && !trivialValueInputTypes.has(node.type)) {
      return true;
    }
    return false;
  }

  /**
   * Checks whether a node can participate in text matching.
   *
   * @param {Node} node Candidate node.
   * @returns {boolean} True when text matching is allowed for the node.
   */
  function isSuitableNodeForTextMatching(node) {
    return !unsuitableNodeNames.has(node.nodeName) && !(document.head && document.head.contains(node));
  }

  /**
   * Clears cached text content for a node and its ancestors.
   *
   * @param {Node|null} node Node whose cached text ancestry should be invalidated.
   */
  function eraseFromCache(node) {
    while (node) {
      textContentCache.delete(node);
      if (node instanceof ShadowRoot) {
        node = node.host;
      } else {
        node = node.parentNode;
      }
    }
  }

  /**
   * Observes text-affecting mutations and invalidates cached text content.
   *
   * @param {MutationRecord[]} mutations DOM mutation records.
   */
  const textChangeObserver = new MutationObserver(mutations => {
    for (const mutation of mutations) {
      eraseFromCache(mutation.target);
    }
  });

  /**
   * Creates or returns cached text content metadata for a root node.
   *
   * @param {Node|ShadowRoot} root Text extraction root.
   * @returns {{full: string, immediate: string[]}} Full and immediate text content.
   */
  function createTextContent(root) {
    let value = textContentCache.get(root);
    if (value) {
      return value;
    }
    value = { full: '', immediate: [] };
    if (!isSuitableNodeForTextMatching(root)) {
      return value;
    }

    let currentImmediate = '';
    if (isNonTrivialValueNode(root)) {
      value.full = root.value;
      value.immediate.push(root.value);
      root.addEventListener(
        'input',
        event => {
          eraseFromCache(event.target);
        },
        { once: true, capture: true }
      );
    } else {
      for (let child = root.firstChild; child; child = child.nextSibling) {
        if (child.nodeType === Node.TEXT_NODE) {
          value.full += child.nodeValue || '';
          currentImmediate += child.nodeValue || '';
          continue;
        }
        if (currentImmediate) {
          value.immediate.push(currentImmediate);
        }
        currentImmediate = '';
        if (child.nodeType === Node.ELEMENT_NODE) {
          value.full += createTextContent(child).full;
        }
      }
      if (currentImmediate) {
        value.immediate.push(currentImmediate);
      }
      if (root instanceof Element && root.shadowRoot) {
        value.full += createTextContent(root.shadowRoot).full;
      }
      if (!observedNodes.has(root)) {
        textChangeObserver.observe(root, {
          childList: true,
          characterData: true,
          subtree: true,
        });
        observedNodes.add(root);
      }
    }
    textContentCache.set(root, value);
    return value;
  }

  injected.isSuitableNodeForTextMatching = isSuitableNodeForTextMatching;
  injected.createTextContent = createTextContent;
  injected.eraseTextContentCache = eraseFromCache;
})(globalThis);
