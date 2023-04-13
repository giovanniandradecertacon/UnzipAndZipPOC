package br.com.certacon.poczipfiles.service;

import br.com.certacon.poczipfiles.model.ZipModel;
import br.com.certacon.poczipfiles.repository.ZipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {UnzipAndZipService.class})
class UnzipAndZipServiceTest {
    @MockBean
    ZipRepository zipRepository;
    @Autowired
    private UnzipAndZipService unzipAndZipService;

    @Test
    void shouldCallUnzipAndZipServiceWhenUnzipandZipFilesReturnWithSuccess() throws IOException {
        //Given
        File source = new File("D:\\PocZipData");
        ZipModel zipModel = ZipModel.builder()
                .sourceFile(source)
                .id(UUID.randomUUID())
                .build();

        List<String> expected = new ArrayList<>();
        //When
        List<String> actual = unzipAndZipService.UnzipFiles(zipModel);
        //Then
        assertEquals(expected, actual);
    }
}