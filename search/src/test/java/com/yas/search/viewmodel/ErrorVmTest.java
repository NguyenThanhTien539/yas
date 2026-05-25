package com.yas.search.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yas.search.viewmodel.error.ErrorVm;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorVmTest {

    @Test
    void compactConstructor_shouldInitializeEmptyFieldErrors() {
        ErrorVm vm = new ErrorVm("400", "Bad Request", "Invalid request");

        assertEquals("400", vm.statusCode());
        assertEquals("Bad Request", vm.title());
        assertEquals("Invalid request", vm.detail());
        assertNotNull(vm.fieldErrors());
        assertTrue(vm.fieldErrors().isEmpty());
    }

    @Test
    void canonicalConstructor_shouldKeepProvidedFieldErrors() {
        ErrorVm vm = new ErrorVm("404", "Not Found", "Missing", List.of("name is required"));

        assertEquals(1, vm.fieldErrors().size());
        assertEquals("name is required", vm.fieldErrors().getFirst());
    }
}
