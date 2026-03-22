package com.yas.media.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileTypeValidatorTest {

    private FileTypeValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @BeforeEach
    void setUp() {
        validator = new FileTypeValidator();

        ValidFileType constraintAnnotation = mock(ValidFileType.class);
        when(constraintAnnotation.allowedTypes())
            .thenReturn(new String[]{"image/jpeg", "image/png", "image/gif"});
        when(constraintAnnotation.message())
            .thenReturn("File type not allowed. Allowed types are: JPEG, PNG, GIF");
        validator.initialize(constraintAnnotation);

        // lenient: only tests that trigger constraint-violation code path will use this stub
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);
    }

    @Test
    void isValid_whenFileIsNull_thenReturnFalse() {
        boolean result = validator.isValid(null, context);
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenContentTypeIsNull_thenReturnFalse() {
        MultipartFile file = new MockMultipartFile("file", "test.png", null, new byte[0]);
        boolean result = validator.isValid(file, context);
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenTypeNotAllowed_thenReturnFalse() {
        MultipartFile file = new MockMultipartFile("file", "test.bmp", "image/bmp", new byte[]{1, 2, 3});
        boolean result = validator.isValid(file, context);
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenTypeAllowedButBytesAreNotValidImage_thenReturnFalse() {
        // Provide jpeg content type but random non-image bytes → ImageIO.read() returns null
        byte[] notImageBytes = "this-is-not-an-image".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", notImageBytes);
        boolean result = validator.isValid(file, context);
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenTypeAllowedAndIOException_thenReturnFalse() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenThrow(new IOException("disk error"));

        boolean result = validator.isValid(file, context);
        assertThat(result).isFalse();
    }

    @Test
    void isValid_whenValidPngImage_thenReturnTrue() throws IOException {
        // Generate real 1x1 PNG bytes using ImageIO so the validator can read it
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        byte[] validPngBytes = baos.toByteArray();

        MultipartFile file = new MockMultipartFile("file", "test.png", "image/png", validPngBytes);
        boolean result = validator.isValid(file, context);
        assertThat(result).isTrue();
    }
}
