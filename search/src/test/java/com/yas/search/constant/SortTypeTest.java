package com.yas.search.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yas.search.constant.enums.SortType;
import org.junit.jupiter.api.Test;

class SortTypeTest {

    @Test
    void enumValues_shouldStayStable() {
        assertEquals(SortType.DEFAULT, SortType.valueOf("DEFAULT"));
        assertEquals(SortType.PRICE_ASC, SortType.valueOf("PRICE_ASC"));
        assertEquals(SortType.PRICE_DESC, SortType.valueOf("PRICE_DESC"));
        assertEquals(3, SortType.values().length);
    }
}
