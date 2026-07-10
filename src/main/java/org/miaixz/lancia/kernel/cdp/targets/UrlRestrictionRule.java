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
package org.miaixz.lancia.kernel.cdp.targets;

import java.util.regex.Pattern;

import org.miaixz.bus.core.lang.Assert;

/**
 * URLPattern-like restriction rule used for target and network filtering.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public final class UrlRestrictionRule {

    /**
     * Original URLPattern rule.
     */
    private final String rule;

    /**
     * Compiled matcher.
     */
    private final Pattern pattern;

    /**
     * Creates a URL restriction rule.
     *
     * @param rule rule
     */
    private UrlRestrictionRule(String rule) {
        this.rule = Assert.notBlank(rule, "rule");
        this.pattern = Pattern.compile(toRegex(this.rule));
    }

    /**
     * Compiles a rule.
     *
     * @param rule rule
     * @return compiled rule
     */
    public static UrlRestrictionRule compile(String rule) {
        return new UrlRestrictionRule(rule);
    }

    /**
     * Returns the original rule.
     *
     * @return rule
     */
    public String rule() {
        return rule;
    }

    /**
     * Returns the test.
     *
     * @param url target URL
     * @return {@code true} when the condition matches
     */
    public boolean test(String url) {
        return url != null && pattern.matcher(url).matches();
    }

    /**
     * Converts the supported URLPattern subset to regex.
     *
     * @param rule rule
     * @return regex
     */
    private String toRegex(String rule) {
        StringBuilder regex = new StringBuilder("^");
        for (int index = 0; index < rule.length(); index++) {
            char current = rule.charAt(index);
            if (current == '*') {
                regex.append(".*");
                continue;
            }
            if (current == ':' && index + 1 < rule.length() && isNameStart(rule.charAt(index + 1))) {
                int end = index + 2;
                while (end < rule.length() && isNamePart(rule.charAt(end))) {
                    end++;
                }
                regex.append("[^/?#]+");
                index = end - 1;
                continue;
            }
            appendEscaped(regex, current);
        }
        regex.append('$');
        return regex.toString();
    }

    /**
     * Returns whether name start is enabled.
     *
     * @param value to use
     * @return {@code true} when the condition matches
     */
    private boolean isNameStart(char value) {
        return Character.isLetter(value) || value == '_';
    }

    /**
     * Returns whether name part is enabled.
     *
     * @param value to use
     * @return {@code true} when the condition matches
     */
    private boolean isNamePart(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    /**
     * Appends a regex-escaped character.
     *
     * @param regex regex
     * @param value character to append
     */
    private void appendEscaped(StringBuilder regex, char value) {
        if ("\\.[]{}()+-^$?|".indexOf(value) >= 0) {
            regex.append('\\');
        }
        regex.append(value);
    }

}
