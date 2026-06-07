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
package org.miaixz.lancia.nimble.emulation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.miaixz.bus.core.lang.Optional;

/**
 * Enumerates Devices.
 *
 * @author Kimi Liu
 * @since Java 17+
 */
public enum Device {

    /**
     * Blackberry PlayBook.
     */
    BLACKBERRY_PLAYBOOK("Blackberry PlayBook",
            "Mozilla/5.0 (PlayBook; U; RIM Tablet OS 2.1.0; en-US) AppleWebKit/536.2+ (KHTML like Gecko) Version/7.2.1.0 Safari/536.2+",
            600, 1024, 1, true, true, false),

    /**
     * Blackberry PlayBook landscape.
     */
    BLACKBERRY_PLAYBOOK_LANDSCAPE("Blackberry PlayBook landscape",
            "Mozilla/5.0 (PlayBook; U; RIM Tablet OS 2.1.0; en-US) AppleWebKit/536.2+ (KHTML like Gecko) Version/7.2.1.0 Safari/536.2+",
            1024, 600, 1, true, true, true),

    /**
     * BlackBerry Z30.
     */
    BLACKBERRY_Z30("BlackBerry Z30",
            "Mozilla/5.0 (BB10; Touch) AppleWebKit/537.10+ (KHTML, like Gecko) Version/10.0.9.2372 Mobile Safari/537.10+",
            360, 640, 2, true, true, false),

    /**
     * BlackBerry Z30 landscape.
     */
    BLACKBERRY_Z30_LANDSCAPE("BlackBerry Z30 landscape",
            "Mozilla/5.0 (BB10; Touch) AppleWebKit/537.10+ (KHTML, like Gecko) Version/10.0.9.2372 Mobile Safari/537.10+",
            640, 360, 2, true, true, true),

    /**
     * Galaxy Note 3.
     */
    GALAXY_NOTE_3("Galaxy Note 3",
            "Mozilla/5.0 (Linux; U; Android 4.3; en-us; SM-N900T Build/JSS15J) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
            360, 640, 3, true, true, false),

    /**
     * Galaxy Note 3 landscape.
     */
    GALAXY_NOTE_3_LANDSCAPE("Galaxy Note 3 landscape",
            "Mozilla/5.0 (Linux; U; Android 4.3; en-us; SM-N900T Build/JSS15J) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
            640, 360, 3, true, true, true),

    /**
     * Galaxy Note II.
     */
    GALAXY_NOTE_II("Galaxy Note II",
            "Mozilla/5.0 (Linux; U; Android 4.1; en-us; GT-N7100 Build/JRO03C) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
            360, 640, 2, true, true, false),

    /**
     * Galaxy Note II landscape.
     */
    GALAXY_NOTE_II_LANDSCAPE("Galaxy Note II landscape",
            "Mozilla/5.0 (Linux; U; Android 4.1; en-us; GT-N7100 Build/JRO03C) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
            640, 360, 2, true, true, true),

    /**
     * Galaxy S III.
     */
    GALAXY_S_III("Galaxy S III",
            "Mozilla/5.0 (Linux; U; Android 4.0; en-us; GT-I9300 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
            360, 640, 2, true, true, false),

    /**
     * Galaxy S III landscape.
     */
    GALAXY_S_III_LANDSCAPE("Galaxy S III landscape",
            "Mozilla/5.0 (Linux; U; Android 4.0; en-us; GT-I9300 Build/IMM76D) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
            640, 360, 2, true, true, true),

    /**
     * Galaxy S5.
     */
    GALAXY_S5("Galaxy S5",
            "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            360, 640, 3, true, true, false),

    /**
     * Galaxy S5 landscape.
     */
    GALAXY_S5_LANDSCAPE("Galaxy S5 landscape",
            "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            640, 360, 3, true, true, true),

    /**
     * Galaxy S8.
     */
    GALAXY_S8("Galaxy S8",
            "Mozilla/5.0 (Linux; Android 7.0; SM-G950U Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.84 Mobile Safari/537.36",
            360, 740, 3, true, true, false),

    /**
     * Galaxy S8 landscape.
     */
    GALAXY_S8_LANDSCAPE("Galaxy S8 landscape",
            "Mozilla/5.0 (Linux; Android 7.0; SM-G950U Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.84 Mobile Safari/537.36",
            740, 360, 3, true, true, true),

    /**
     * Galaxy S9+.
     */
    GALAXY_S9_PLUS("Galaxy S9+",
            "Mozilla/5.0 (Linux; Android 8.0.0; SM-G965U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.111 Mobile Safari/537.36",
            320, 658, 4.5, true, true, false),

    /**
     * Galaxy S9+ landscape.
     */
    GALAXY_S9_PLUS_LANDSCAPE("Galaxy S9+ landscape",
            "Mozilla/5.0 (Linux; Android 8.0.0; SM-G965U Build/R16NW) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.111 Mobile Safari/537.36",
            658, 320, 4.5, true, true, true),

    /**
     * Galaxy Tab S4.
     */
    GALAXY_TAB_S4("Galaxy Tab S4",
            "Mozilla/5.0 (Linux; Android 8.1.0; SM-T837A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.80 Safari/537.36",
            712, 1138, 2.25, true, true, false),

    /**
     * Galaxy Tab S4 landscape.
     */
    GALAXY_TAB_S4_LANDSCAPE("Galaxy Tab S4 landscape",
            "Mozilla/5.0 (Linux; Android 8.1.0; SM-T837A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.80 Safari/537.36",
            1138, 712, 2.25, true, true, true),

    /**
     * iPad.
     */
    IPAD("iPad",
            "Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1",
            768, 1024, 2, true, true, false),

    /**
     * iPad landscape.
     */
    IPAD_LANDSCAPE("iPad landscape",
            "Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1",
            1024, 768, 2, true, true, true),

    /**
     * iPad (gen 6).
     */
    IPAD_GEN_6("iPad (gen 6)",
            "Mozilla/5.0 (iPad; CPU OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            768, 1024, 2, true, true, false),

    /**
     * iPad (gen 6) landscape.
     */
    IPAD_GEN_6_LANDSCAPE("iPad (gen 6) landscape",
            "Mozilla/5.0 (iPad; CPU OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            1024, 768, 2, true, true, true),

    /**
     * iPad (gen 7).
     */
    IPAD_GEN_7("iPad (gen 7)",
            "Mozilla/5.0 (iPad; CPU OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            810, 1080, 2, true, true, false),

    /**
     * iPad (gen 7) landscape.
     */
    IPAD_GEN_7_LANDSCAPE("iPad (gen 7) landscape",
            "Mozilla/5.0 (iPad; CPU OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            1080, 810, 2, true, true, true),

    /**
     * iPad Mini.
     */
    IPAD_MINI("iPad Mini",
            "Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1",
            768, 1024, 2, true, true, false),

    /**
     * iPad Mini landscape.
     */
    IPAD_MINI_LANDSCAPE("iPad Mini landscape",
            "Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1",
            1024, 768, 2, true, true, true),

    /**
     * iPad Pro.
     */
    IPAD_PRO("iPad Pro",
            "Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1",
            1024, 1366, 2, true, true, false),

    /**
     * iPad Pro landscape.
     */
    IPAD_PRO_LANDSCAPE("iPad Pro landscape",
            "Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1",
            1366, 1024, 2, true, true, true),

    /**
     * iPad Pro 11.
     */
    IPAD_PRO_11("iPad Pro 11",
            "Mozilla/5.0 (iPad; CPU OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            834, 1194, 2, true, true, false),

    /**
     * iPad Pro 11 landscape.
     */
    IPAD_PRO_11_LANDSCAPE("iPad Pro 11 landscape",
            "Mozilla/5.0 (iPad; CPU OS 12_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            1194, 834, 2, true, true, true),

    /**
     * iPhone 4.
     */
    IPHONE_4("iPhone 4",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_2 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D257 Safari/9537.53",
            320, 480, 2, true, true, false),

    /**
     * iPhone 4 landscape.
     */
    IPHONE_4_LANDSCAPE("iPhone 4 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 7_1_2 like Mac OS X) AppleWebKit/537.51.2 (KHTML, like Gecko) Version/7.0 Mobile/11D257 Safari/9537.53",
            480, 320, 2, true, true, true),

    /**
     * iPhone 5.
     */
    IPHONE_5("iPhone 5",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1",
            320, 568, 2, true, true, false),

    /**
     * iPhone 5 landscape.
     */
    IPHONE_5_LANDSCAPE("iPhone 5 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1",
            568, 320, 2, true, true, true),

    /**
     * iPhone 6.
     */
    IPHONE_6("iPhone 6",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            375, 667, 2, true, true, false),

    /**
     * iPhone 6 landscape.
     */
    IPHONE_6_LANDSCAPE("iPhone 6 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            667, 375, 2, true, true, true),

    /**
     * iPhone 6 Plus.
     */
    IPHONE_6_PLUS("iPhone 6 Plus",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            414, 736, 3, true, true, false),

    /**
     * iPhone 6 Plus landscape.
     */
    IPHONE_6_PLUS_LANDSCAPE("iPhone 6 Plus landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            736, 414, 3, true, true, true),

    /**
     * iPhone 7.
     */
    IPHONE_7("iPhone 7",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            375, 667, 2, true, true, false),

    /**
     * iPhone 7 landscape.
     */
    IPHONE_7_LANDSCAPE("iPhone 7 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            667, 375, 2, true, true, true),

    /**
     * iPhone 7 Plus.
     */
    IPHONE_7_PLUS("iPhone 7 Plus",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            414, 736, 3, true, true, false),

    /**
     * iPhone 7 Plus landscape.
     */
    IPHONE_7_PLUS_LANDSCAPE("iPhone 7 Plus landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            736, 414, 3, true, true, true),

    /**
     * iPhone 8.
     */
    IPHONE_8("iPhone 8",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            375, 667, 2, true, true, false),

    /**
     * iPhone 8 landscape.
     */
    IPHONE_8_LANDSCAPE("iPhone 8 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            667, 375, 2, true, true, true),

    /**
     * iPhone 8 Plus.
     */
    IPHONE_8_PLUS("iPhone 8 Plus",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            414, 736, 3, true, true, false),

    /**
     * iPhone 8 Plus landscape.
     */
    IPHONE_8_PLUS_LANDSCAPE("iPhone 8 Plus landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            736, 414, 3, true, true, true),

    /**
     * iPhone SE.
     */
    IPHONE_SE("iPhone SE",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1",
            320, 568, 2, true, true, false),

    /**
     * iPhone SE landscape.
     */
    IPHONE_SE_LANDSCAPE("iPhone SE landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_1 like Mac OS X) AppleWebKit/603.1.30 (KHTML, like Gecko) Version/10.0 Mobile/14E304 Safari/602.1",
            568, 320, 2, true, true, true),

    /**
     * iPhone X.
     */
    IPHONE_X("iPhone X",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            375, 812, 3, true, true, false),

    /**
     * iPhone X landscape.
     */
    IPHONE_X_LANDSCAPE("iPhone X landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1",
            812, 375, 3, true, true, true),

    /**
     * iPhone XR.
     */
    IPHONE_XR("iPhone XR",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Mobile/15E148 Safari/604.1",
            414, 896, 3, true, true, false),

    /**
     * iPhone XR landscape.
     */
    IPHONE_XR_LANDSCAPE("iPhone XR landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 12_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/12.0 Mobile/15E148 Safari/604.1",
            896, 414, 3, true, true, true),

    /**
     * iPhone 11.
     */
    IPHONE_11("iPhone 11",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1 Mobile/15E148 Safari/604.1",
            414, 828, 2, true, true, false),

    /**
     * iPhone 11 landscape.
     */
    IPHONE_11_LANDSCAPE("iPhone 11 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1 Mobile/15E148 Safari/604.1",
            828, 414, 2, true, true, true),

    /**
     * iPhone 11 Pro.
     */
    IPHONE_11_PRO("iPhone 11 Pro",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1 Mobile/15E148 Safari/604.1",
            375, 812, 3, true, true, false),

    /**
     * iPhone 11 Pro landscape.
     */
    IPHONE_11_PRO_LANDSCAPE("iPhone 11 Pro landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1 Mobile/15E148 Safari/604.1",
            812, 375, 3, true, true, true),

    /**
     * iPhone 11 Pro Max.
     */
    IPHONE_11_PRO_MAX("iPhone 11 Pro Max",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1 Mobile/15E148 Safari/604.1",
            414, 896, 3, true, true, false),

    /**
     * iPhone 11 Pro Max landscape.
     */
    IPHONE_11_PRO_MAX_LANDSCAPE("iPhone 11 Pro Max landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 13_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1 Mobile/15E148 Safari/604.1",
            896, 414, 3, true, true, true),

    /**
     * iPhone 12.
     */
    IPHONE_12("iPhone 12",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            390, 844, 3, true, true, false),

    /**
     * iPhone 12 landscape.
     */
    IPHONE_12_LANDSCAPE("iPhone 12 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            844, 390, 3, true, true, true),

    /**
     * iPhone 12 Pro.
     */
    IPHONE_12_PRO("iPhone 12 Pro",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            390, 844, 3, true, true, false),

    /**
     * iPhone 12 Pro landscape.
     */
    IPHONE_12_PRO_LANDSCAPE("iPhone 12 Pro landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            844, 390, 3, true, true, true),

    /**
     * iPhone 12 Pro Max.
     */
    IPHONE_12_PRO_MAX("iPhone 12 Pro Max",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            428, 926, 3, true, true, false),

    /**
     * iPhone 12 Pro Max landscape.
     */
    IPHONE_12_PRO_MAX_LANDSCAPE("iPhone 12 Pro Max landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            926, 428, 3, true, true, true),

    /**
     * iPhone 12 Mini.
     */
    IPHONE_12_MINI("iPhone 12 Mini",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            375, 812, 3, true, true, false),

    /**
     * iPhone 12 Mini landscape.
     */
    IPHONE_12_MINI_LANDSCAPE("iPhone 12 Mini landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 14_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            812, 375, 3, true, true, true),

    /**
     * iPhone 13.
     */
    IPHONE_13("iPhone 13",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            390, 844, 3, true, true, false),

    /**
     * iPhone 13 landscape.
     */
    IPHONE_13_LANDSCAPE("iPhone 13 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            844, 390, 3, true, true, true),

    /**
     * iPhone 13 Pro.
     */
    IPHONE_13_PRO("iPhone 13 Pro",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            390, 844, 3, true, true, false),

    /**
     * iPhone 13 Pro landscape.
     */
    IPHONE_13_PRO_LANDSCAPE("iPhone 13 Pro landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            844, 390, 3, true, true, true),

    /**
     * iPhone 13 Pro Max.
     */
    IPHONE_13_PRO_MAX("iPhone 13 Pro Max",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            428, 926, 3, true, true, false),

    /**
     * iPhone 13 Pro Max landscape.
     */
    IPHONE_13_PRO_MAX_LANDSCAPE("iPhone 13 Pro Max landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            926, 428, 3, true, true, true),

    /**
     * iPhone 13 Mini.
     */
    IPHONE_13_MINI("iPhone 13 Mini",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            375, 812, 3, true, true, false),

    /**
     * iPhone 13 Mini landscape.
     */
    IPHONE_13_MINI_LANDSCAPE("iPhone 13 Mini landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Mobile/15E148 Safari/604.1",
            812, 375, 3, true, true, true),

    /**
     * iPhone 14.
     */
    IPHONE_14("iPhone 14",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            390, 663, 3, true, true, false),

    /**
     * iPhone 14 landscape.
     */
    IPHONE_14_LANDSCAPE("iPhone 14 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            750, 340, 3, true, true, true),

    /**
     * iPhone 14 Plus.
     */
    IPHONE_14_PLUS("iPhone 14 Plus",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            428, 745, 3, true, true, false),

    /**
     * iPhone 14 Plus landscape.
     */
    IPHONE_14_PLUS_LANDSCAPE("iPhone 14 Plus landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            832, 378, 3, true, true, true),

    /**
     * iPhone 14 Pro.
     */
    IPHONE_14_PRO("iPhone 14 Pro",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            393, 659, 3, true, true, false),

    /**
     * iPhone 14 Pro landscape.
     */
    IPHONE_14_PRO_LANDSCAPE("iPhone 14 Pro landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            734, 343, 3, true, true, true),

    /**
     * iPhone 14 Pro Max.
     */
    IPHONE_14_PRO_MAX("iPhone 14 Pro Max",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            430, 739, 3, true, true, false),

    /**
     * iPhone 14 Pro Max landscape.
     */
    IPHONE_14_PRO_MAX_LANDSCAPE("iPhone 14 Pro Max landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
            814, 380, 3, true, true, true),

    /**
     * iPhone 15.
     */
    IPHONE_15("iPhone 15",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            393, 659, 3, true, true, false),

    /**
     * iPhone 15 landscape.
     */
    IPHONE_15_LANDSCAPE("iPhone 15 landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            734, 343, 3, true, true, true),

    /**
     * iPhone 15 Plus.
     */
    IPHONE_15_PLUS("iPhone 15 Plus",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            430, 739, 3, true, true, false),

    /**
     * iPhone 15 Plus landscape.
     */
    IPHONE_15_PLUS_LANDSCAPE("iPhone 15 Plus landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            814, 380, 3, true, true, true),

    /**
     * iPhone 15 Pro.
     */
    IPHONE_15_PRO("iPhone 15 Pro",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            393, 659, 3, true, true, false),

    /**
     * iPhone 15 Pro landscape.
     */
    IPHONE_15_PRO_LANDSCAPE("iPhone 15 Pro landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            734, 343, 3, true, true, true),

    /**
     * iPhone 15 Pro Max.
     */
    IPHONE_15_PRO_MAX("iPhone 15 Pro Max",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            430, 739, 3, true, true, false),

    /**
     * iPhone 15 Pro Max landscape.
     */
    IPHONE_15_PRO_MAX_LANDSCAPE("iPhone 15 Pro Max landscape",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1",
            814, 380, 3, true, true, true),

    /**
     * JioPhone 2.
     */
    JIOPHONE_2("JioPhone 2",
            "Mozilla/5.0 (Mobile; LYF/F300B/LYF-F300B-001-01-15-130718-i;Android; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5",
            240, 320, 1, true, true, false),

    /**
     * JioPhone 2 landscape.
     */
    JIOPHONE_2_LANDSCAPE("JioPhone 2 landscape",
            "Mozilla/5.0 (Mobile; LYF/F300B/LYF-F300B-001-01-15-130718-i;Android; rv:48.0) Gecko/48.0 Firefox/48.0 KAIOS/2.5",
            320, 240, 1, true, true, true),

    /**
     * Kindle Fire HDX.
     */
    KINDLE_FIRE_HDX("Kindle Fire HDX",
            "Mozilla/5.0 (Linux; U; en-us; KFAPWI Build/JDQ39) AppleWebKit/535.19 (KHTML, like Gecko) Silk/3.13 Safari/535.19 Silk-Accelerated=true",
            800, 1280, 2, true, true, false),

    /**
     * Kindle Fire HDX landscape.
     */
    KINDLE_FIRE_HDX_LANDSCAPE("Kindle Fire HDX landscape",
            "Mozilla/5.0 (Linux; U; en-us; KFAPWI Build/JDQ39) AppleWebKit/535.19 (KHTML, like Gecko) Silk/3.13 Safari/535.19 Silk-Accelerated=true",
            1280, 800, 2, true, true, true),

    /**
     * LG Optimus L70.
     */
    LG_OPTIMUS_L70("LG Optimus L70",
            "Mozilla/5.0 (Linux; U; Android 4.4.2; en-us; LGMS323 Build/KOT49I.MS32310c) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/75.0.3765.0 Mobile Safari/537.36",
            384, 640, 1.25, true, true, false),

    /**
     * LG Optimus L70 landscape.
     */
    LG_OPTIMUS_L70_LANDSCAPE("LG Optimus L70 landscape",
            "Mozilla/5.0 (Linux; U; Android 4.4.2; en-us; LGMS323 Build/KOT49I.MS32310c) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/75.0.3765.0 Mobile Safari/537.36",
            640, 384, 1.25, true, true, true),

    /**
     * Microsoft Lumia 550.
     */
    MICROSOFT_LUMIA_550("Microsoft Lumia 550",
            "Mozilla/5.0 (Windows Phone 10.0; Android 4.2.1; Microsoft; Lumia 550) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Mobile Safari/537.36 Edge/14.14263",
            640, 360, 2, true, true, false),

    /**
     * Microsoft Lumia 950.
     */
    MICROSOFT_LUMIA_950("Microsoft Lumia 950",
            "Mozilla/5.0 (Windows Phone 10.0; Android 4.2.1; Microsoft; Lumia 950) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Mobile Safari/537.36 Edge/14.14263",
            360, 640, 4, true, true, false),

    /**
     * Microsoft Lumia 950 landscape.
     */
    MICROSOFT_LUMIA_950_LANDSCAPE("Microsoft Lumia 950 landscape",
            "Mozilla/5.0 (Windows Phone 10.0; Android 4.2.1; Microsoft; Lumia 950) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Mobile Safari/537.36 Edge/14.14263",
            640, 360, 4, true, true, true),

    /**
     * Nexus 10.
     */
    NEXUS_10("Nexus 10",
            "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 10 Build/MOB31T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Safari/537.36",
            800, 1280, 2, true, true, false),

    /**
     * Nexus 10 landscape.
     */
    NEXUS_10_LANDSCAPE("Nexus 10 landscape",
            "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 10 Build/MOB31T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Safari/537.36",
            1280, 800, 2, true, true, true),

    /**
     * Nexus 4.
     */
    NEXUS_4("Nexus 4",
            "Mozilla/5.0 (Linux; Android 4.4.2; Nexus 4 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            384, 640, 2, true, true, false),

    /**
     * Nexus 4 landscape.
     */
    NEXUS_4_LANDSCAPE("Nexus 4 landscape",
            "Mozilla/5.0 (Linux; Android 4.4.2; Nexus 4 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            640, 384, 2, true, true, true),

    /**
     * Nexus 5.
     */
    NEXUS_5("Nexus 5",
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            360, 640, 3, true, true, false),

    /**
     * Nexus 5 landscape.
     */
    NEXUS_5_LANDSCAPE("Nexus 5 landscape",
            "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            640, 360, 3, true, true, true),

    /**
     * Nexus 5X.
     */
    NEXUS_5X("Nexus 5X",
            "Mozilla/5.0 (Linux; Android 8.0.0; Nexus 5X Build/OPR4.170623.006) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            412, 732, 2.625, true, true, false),

    /**
     * Nexus 5X landscape.
     */
    NEXUS_5X_LANDSCAPE("Nexus 5X landscape",
            "Mozilla/5.0 (Linux; Android 8.0.0; Nexus 5X Build/OPR4.170623.006) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            732, 412, 2.625, true, true, true),

    /**
     * Nexus 6.
     */
    NEXUS_6("Nexus 6",
            "Mozilla/5.0 (Linux; Android 7.1.1; Nexus 6 Build/N6F26U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            412, 732, 3.5, true, true, false),

    /**
     * Nexus 6 landscape.
     */
    NEXUS_6_LANDSCAPE("Nexus 6 landscape",
            "Mozilla/5.0 (Linux; Android 7.1.1; Nexus 6 Build/N6F26U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            732, 412, 3.5, true, true, true),

    /**
     * Nexus 6P.
     */
    NEXUS_6P("Nexus 6P",
            "Mozilla/5.0 (Linux; Android 8.0.0; Nexus 6P Build/OPP3.170518.006) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            412, 732, 3.5, true, true, false),

    /**
     * Nexus 6P landscape.
     */
    NEXUS_6P_LANDSCAPE("Nexus 6P landscape",
            "Mozilla/5.0 (Linux; Android 8.0.0; Nexus 6P Build/OPP3.170518.006) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            732, 412, 3.5, true, true, true),

    /**
     * Nexus 7.
     */
    NEXUS_7("Nexus 7",
            "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 7 Build/MOB30X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Safari/537.36",
            600, 960, 2, true, true, false),

    /**
     * Nexus 7 landscape.
     */
    NEXUS_7_LANDSCAPE("Nexus 7 landscape",
            "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 7 Build/MOB30X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Safari/537.36",
            960, 600, 2, true, true, true),

    /**
     * Nokia Lumia 520.
     */
    NOKIA_LUMIA_520("Nokia Lumia 520",
            "Mozilla/5.0 (compatible; MSIE 10.0; Windows Phone 8.0; Trident/6.0; IEMobile/10.0; ARM; Touch; NOKIA; Lumia 520)",
            320, 533, 1.5, true, true, false),

    /**
     * Nokia Lumia 520 landscape.
     */
    NOKIA_LUMIA_520_LANDSCAPE("Nokia Lumia 520 landscape",
            "Mozilla/5.0 (compatible; MSIE 10.0; Windows Phone 8.0; Trident/6.0; IEMobile/10.0; ARM; Touch; NOKIA; Lumia 520)",
            533, 320, 1.5, true, true, true),

    /**
     * Nokia N9.
     */
    NOKIA_N9("Nokia N9",
            "Mozilla/5.0 (MeeGo; NokiaN9) AppleWebKit/534.13 (KHTML, like Gecko) NokiaBrowser/8.5.0 Mobile Safari/534.13",
            480, 854, 1, true, true, false),

    /**
     * Nokia N9 landscape.
     */
    NOKIA_N9_LANDSCAPE("Nokia N9 landscape",
            "Mozilla/5.0 (MeeGo; NokiaN9) AppleWebKit/534.13 (KHTML, like Gecko) NokiaBrowser/8.5.0 Mobile Safari/534.13",
            854, 480, 1, true, true, true),

    /**
     * Pixel 2.
     */
    PIXEL_2("Pixel 2",
            "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            411, 731, 2.625, true, true, false),

    /**
     * Pixel 2 landscape.
     */
    PIXEL_2_LANDSCAPE("Pixel 2 landscape",
            "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            731, 411, 2.625, true, true, true),

    /**
     * Pixel 2 XL.
     */
    PIXEL_2_XL("Pixel 2 XL",
            "Mozilla/5.0 (Linux; Android 8.0.0; Pixel 2 XL Build/OPD1.170816.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            411, 823, 3.5, true, true, false),

    /**
     * Pixel 2 XL landscape.
     */
    PIXEL_2_XL_LANDSCAPE("Pixel 2 XL landscape",
            "Mozilla/5.0 (Linux; Android 8.0.0; Pixel 2 XL Build/OPD1.170816.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3765.0 Mobile Safari/537.36",
            823, 411, 3.5, true, true, true),

    /**
     * Pixel 3.
     */
    PIXEL_3("Pixel 3",
            "Mozilla/5.0 (Linux; Android 9; Pixel 3 Build/PQ1A.181105.017.A1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36",
            393, 786, 2.75, true, true, false),

    /**
     * Pixel 3 landscape.
     */
    PIXEL_3_LANDSCAPE("Pixel 3 landscape",
            "Mozilla/5.0 (Linux; Android 9; Pixel 3 Build/PQ1A.181105.017.A1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.158 Mobile Safari/537.36",
            786, 393, 2.75, true, true, true),

    /**
     * Pixel 4.
     */
    PIXEL_4("Pixel 4",
            "Mozilla/5.0 (Linux; Android 10; Pixel 4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Mobile Safari/537.36",
            353, 745, 3, true, true, false),

    /**
     * Pixel 4 landscape.
     */
    PIXEL_4_LANDSCAPE("Pixel 4 landscape",
            "Mozilla/5.0 (Linux; Android 10; Pixel 4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Mobile Safari/537.36",
            745, 353, 3, true, true, true),

    /**
     * Pixel 4a (5G).
     */
    PIXEL_4A_5G("Pixel 4a (5G)",
            "Mozilla/5.0 (Linux; Android 11; Pixel 4a (5G)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4812.0 Mobile Safari/537.36",
            353, 745, 3, true, true, false),

    /**
     * Pixel 4a (5G) landscape.
     */
    PIXEL_4A_5G_LANDSCAPE("Pixel 4a (5G) landscape",
            "Mozilla/5.0 (Linux; Android 11; Pixel 4a (5G)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4812.0 Mobile Safari/537.36",
            745, 353, 3, true, true, true),

    /**
     * Pixel 5.
     */
    PIXEL_5("Pixel 5",
            "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4812.0 Mobile Safari/537.36",
            393, 851, 3, true, true, false),

    /**
     * Pixel 5 landscape.
     */
    PIXEL_5_LANDSCAPE("Pixel 5 landscape",
            "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4812.0 Mobile Safari/537.36",
            851, 393, 3, true, true, true),

    /**
     * Moto G4.
     */
    MOTO_G4("Moto G4",
            "Mozilla/5.0 (Linux; Android 7.0; Moto G (4)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4812.0 Mobile Safari/537.36",
            360, 640, 3, true, true, false),

    /**
     * Moto G4 landscape.
     */
    MOTO_G4_LANDSCAPE("Moto G4 landscape",
            "Mozilla/5.0 (Linux; Android 7.0; Moto G (4)) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4812.0 Mobile Safari/537.36",
            640, 360, 3, true, true, true),

    /**
     * Desktop Chrome.
     */
    DESKTOP_CHROME("Desktop Chrome",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            1280, 720, 1, false, false, false);

    /**
     * Shared constant for known devices.
     */
    private static final Map<String, Device> KNOWN_DEVICES;

    static {
        Map<String, Device> devices = new LinkedHashMap<>();
        for (Device device : values()) {
            devices.put(device.deviceName, device);
        }
        KNOWN_DEVICES = Collections.unmodifiableMap(devices);
    }
    /**
     * Current device name.
     */
    private final String deviceName;

    /**
     * User-Agent.
     */
    private final String userAgent;
    /**
     * Current width.
     */
    private final int width;
    /**
     * Current height.
     */
    private final int height;
    /**
     * Current scale factor.
     */
    private final double scaleFactor;
    /**
     * Whether mobile is enabled.
     */
    private final boolean mobile;
    /**
     * Whether has touch is enabled.
     */
    private final boolean hasTouch;
    /**
     * Whether landscape is enabled.
     */
    private final boolean landscape;

    /**
     * Creates a device.
     *
     * @param deviceName  device name
     * @param userAgent   user agent
     * @param width       width
     * @param height      height
     * @param scaleFactor scale factor
     * @param mobile      mobile
     * @param hasTouch    has touch
     * @param landscape   landscape
     */
    Device(String deviceName, String userAgent, int width, int height, double scaleFactor, boolean mobile,
            boolean hasTouch, boolean landscape) {
        this.deviceName = deviceName;
        this.userAgent = userAgent;
        this.width = width;
        this.height = height;
        this.scaleFactor = scaleFactor;
        this.mobile = mobile;
        this.hasTouch = hasTouch;
        this.landscape = landscape;
    }

    /**
     * Returns the known devices.
     *
     * @return mapped values
     */
    public static Map<String, Device> knownDevices() {
        return KNOWN_DEVICES;
    }

    /**
     * Returns the names.
     *
     * @return values
     */
    public static List<String> names() {
        return List.copyOf(KNOWN_DEVICES.keySet());
    }

    /**
     * Creates this value from name.
     *
     * @param name name to use
     * @return optional value
     */
    public static Optional<Device> fromName(String name) {
        return Optional.ofNullable(KNOWN_DEVICES.get(name));
    }

    /**
     * Returns the of.
     *
     * @param name name to use
     * @return of value
     */
    public static Device of(String name) {
        return fromName(name).orElseThrow(() -> new IllegalArgumentException("Unknown device name: " + name));
    }

    /**
     * Returns the device name.
     *
     * @return device name value
     */
    public String deviceName() {
        return deviceName;
    }

    /**
     * Returns the device name.
     *
     * @return device name
     */
    public String getDeviceName() {
        return deviceName;
    }

    /**
     * Returns the user agent.
     *
     * @return user agent value
     */
    public String userAgent() {
        return userAgent;
    }

    /**
     * Returns the user agent.
     *
     * @return user agent
     */
    public String getUserAgent() {
        return userAgent;
    }

    /**
     * Returns the viewport.
     *
     * @return viewport value
     */
    public Viewport viewport() {
        Viewport viewport = new Viewport();
        viewport.setWidth(width);
        viewport.setHeight(height);
        viewport.setDeviceScaleFactor(scaleFactor);
        viewport.setMobile(mobile);
        viewport.setHasTouch(hasTouch);
        viewport.setLandscape(landscape);
        return viewport;
    }

}
