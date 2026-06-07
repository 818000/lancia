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
 * Installs the ARIA query selector runtime into the shared injected registry.
 *
 * @param {typeof globalThis} globalThis Browser global object that owns the injected registry.
 */
(function installARIAQuerySelector(globalThis) {
  const injected = globalThis.__lanciaInjected || (globalThis.__lanciaInjected = {});

  /**
   * Parses an ARIA selector into role and accessible-name constraints.
   *
   * @param {string} selector ARIA selector expression.
   * @returns {{name: string|undefined, role: string|undefined}} Parsed selector constraints.
   */
  function parseSelector(selector) {
    const attributePattern = /\[\s*(name|role)\s*=\s*(["'])(.*?)\2\s*\]/g;
    const stripAttributePattern = /\[\s*(name|role)\s*=\s*(["'])(.*?)\2\s*\]/g;
    const result = {
      name: undefined,
      role: undefined,
    };
    let match;
    while ((match = attributePattern.exec(selector))) {
      result[match[1]] = match[3];
    }
    const defaultName = selector.replace(stripAttributePattern, '').trim();
    if (defaultName && !result.name) {
      result.name = defaultName;
    }
    return result;
  }

  /**
   * Resolves the fallback accessible name used by the lightweight ARIA matcher.
   *
   * @param {Element} node Candidate element.
   * @returns {string} Accessible name approximation.
   */
  function accessibleName(node) {
    return node.getAttribute('aria-label') || node.getAttribute('title') || node.textContent || '';
  }

  /**
   * Finds all elements that satisfy the fallback ARIA selector constraints.
   *
   * @param {ParentNode|Element} root Search root.
   * @param {string} selector ARIA selector expression.
   * @returns {Element[]} Matching elements.
   */
  function fallbackQuerySelectorAll(root, selector) {
    const query = parseSelector(selector);
    const nodes = [];
    if (root instanceof Element) {
      nodes.push(root);
    }
    if (root.querySelectorAll) {
      nodes.push(...root.querySelectorAll('*'));
    }
    return nodes.filter(node => {
      if (query.role && node.getAttribute('role') !== query.role) {
        return false;
      }
      return !query.name || accessibleName(node).includes(query.name);
    });
  }

  /**
   * Queries the first element matching an ARIA selector.
   *
   * @param {ParentNode|Element} root Search root.
   * @param {string} selector ARIA selector expression.
   * @returns {Promise<Element|null>} Matching element, or null when no element matches.
   */
  injected.ariaQuerySelector = async function ariaQuerySelector(root, selector) {
    if (globalThis.__ariaQuerySelector) {
      return await globalThis.__ariaQuerySelector(root, selector);
    }
    const nodes = fallbackQuerySelectorAll(root, selector);
    return nodes.length ? nodes[0] : null;
  };

  /**
   * Iterates over every element matching an ARIA selector.
   *
   * @param {ParentNode|Element} root Search root.
   * @param {string} selector ARIA selector expression.
   * @yields {Element} Matching element.
   */
  injected.ariaQuerySelectorAll = async function* ariaQuerySelectorAll(root, selector) {
    const nodes = globalThis.__ariaQuerySelectorAll
      ? await globalThis.__ariaQuerySelectorAll(root, selector)
      : fallbackQuerySelectorAll(root, selector);
    yield* nodes;
  };
})(globalThis);
