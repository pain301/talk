package com.smart.resource;

import org.springframework.core.io.*;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.FileCopyUtils;

import java.io.*;

/**
 * Created by meilb on 2017/8/22.
 */
public class TestResource {

    public static void test_classpath_res() throws IOException {
        Resource res = new ClassPathResource("conf/a.txt");
        EncodedResource encRes = new EncodedResource(res, "UTF-8");
        String content = FileCopyUtils.copyToString(encRes.getReader());
        System.out.println(content);
    }

    public static void test_filepath_res() throws IOException {
        String filepath = "D://BugReport.txt";
        WritableResource res = new PathResource(filepath);
        OutputStream out = res.getOutputStream();
        out.write("file path resource".getBytes());
        out.close();

        InputStream in = res.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int tmp;
        while (-1 != (tmp = in.read())) {
            baos.write(tmp);
        }
        System.out.println(baos.toString());
        System.out.println(res.getFilename());
        in.close();
        baos.close();
    }

    public static void test_default() throws IOException {
        InputStream in = (new DefaultResourceLoader()).getResource("classpath:/conf/a.txt").getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int tmp;
        while (-1 != (tmp = in.read())) {
            baos.write(tmp);
        }
        System.out.println(baos.toString());
        in.close();
        baos.close();
        baos = new ByteArrayOutputStream();
        in = (new DefaultResourceLoader()).getResource("file:D://BugReport.txt").getInputStream();
        while (-1 != (tmp = in.read())) {
            baos.write(tmp);
        }
        System.out.println(baos.toString());
    }

    public static void main(String[] args) throws IOException {
//        test_classpath_res();
//        test_filepath_res();
        test_default();
    }
}
