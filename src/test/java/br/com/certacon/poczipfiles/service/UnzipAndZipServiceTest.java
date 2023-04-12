package br.com.certacon.poczipfiles.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {UnzipAndZipService.class})
class UnzipAndZipServiceTest {
    @Autowired
    private UnzipAndZipService unzipAndZipService;


    @Test
    void shouldCallUnzipAndZipServiceWhenUnzipandZipFilesReturnWithSuccess() throws IOException {
        //Given
        File path = new File("D:\\BASE TESTES AUTOMAÇÃO\\PocZipData");
        List<String> expected = new ArrayList<>();
        //When
        List<String> actual = unzipAndZipService.UnzipFiles(path);
        //Then
        assertEquals(expected, actual);
    }
}