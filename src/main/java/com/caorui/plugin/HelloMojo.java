package com.caorui.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.*;
import java.security.MessageDigest;

/**
 * 自定义插件
 * 功能：取消css、js、html页面的缓存
 */
@Mojo(name="hello",defaultPhase= LifecyclePhase.PACKAGE)
public class HelloMojo extends AbstractMojo {
    //版本号
    private String version;

    //文件后缀
    @Parameter(defaultValue = ".jsp,.html,.css")
    private String suffix;

    //扫描的文件夹
    @Parameter(property = "project.basedir")
    private String file;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File f = new File(file);
        scanFile(f);
    }

    /**
     * 扫描文件
     *
     * @param file
     */
    private void scanFile(File file) {
        if (!file.exists()) {//判断文件是否存在，不存在直接返回
            return;
        }
        //判断文件是否为文件夹
        File[] files = file.listFiles();
        for(File f:files){
            if (f.isDirectory()) {//是文件夹
                scanFile(f);
            } else {//是文件
                isStipulation(f);
            }
        }

    }

    /**
     * 判断文件是否符合要求
     *
     * @param file
     * @return
     */
    private void isStipulation(File file) {
        //获取文件名
        String fileName = file.getName();
        //获取需要取消缓存的文件名后缀
        String[] split = suffix.split(",");
        for (String suf : split) {
            if (fileName.endsWith(suf)) {//文件名后缀符合要求
                //TODO
                //对文件hash
                String version = hashToFile(file);
                //读取文件的每一行
                replace(file,version);
            }
        }
    }

    /**
     * 对文件hash
     *
     * @param file
     * @return
     */
    private String hashToFile(File file) {
        FileInputStream fis = null;
        try {
            //编码类
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            //创建文件流
            fis = new FileInputStream(file);
            byte[] bytes = new byte[1024];
            int num = 0;
            //将文件成字节数组
            do {
                num = fis.read(bytes);
                if (num > 0) {
                    messageDigest.update(bytes, 0, num);
                }
            } while (num != -1);
            //返回
            byte[] decodeBytes = messageDigest.digest();
            //计算编码的字符串
            String result = "";
            for (int i = 0; i < decodeBytes.length; i++) {
                result += Integer.toString((decodeBytes[i] & 0xff) + 0x100, 16).substring(1);
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 更改文件中静态资源
     */
    private void replace(File file, String version) {
        //获取hash的版本后10位
        version = version.substring(25);
        //读IO
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        //写IO
        File wfile = new File(file.getName()+"-w");
//        FileOutputStream fos = null;
        PrintWriter pw = null;
        try {
            //读IO
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis);
            br = new BufferedReader(isr);
            //写IO
            pw = new PrintWriter(wfile);

            String line = null;
            //读文件行
            while ((line = br.readLine()) != null) {
                //判断文件是否是script或者css引用
                String scrStart = "<script";
                String cssStart = "<link";
                line = line.trim();//去除空格
                if (line.startsWith(scrStart)) {//javaScript
                    //匹配为双引号的情况
                    line = line.replaceAll("\\.js(\\?v=\\w*)?[\"]", ".js?v="+version+"\"");
                    //匹配为单引号的情况
                    line = line.replaceAll("\\.js(\\?v=\\w*)?[\']", ".js?v="+version+"\'");

                } else if (line.startsWith(cssStart)) {
                    //替换.css
                    //匹配为双引号的情况
                    line = line.replaceAll("\\.css(\\?v=\\w*)?[\"]", ".css?v="+version+"\"");
                    //匹配为单引号的情况
                    line = line.replaceAll("\\.css(\\?v=\\w*)?[\']", ".css?v="+version+"\'");
                }
                pw.println(line);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            //关闭流
            if (fis != null) {
                try {
                    fis.close();
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(pw!=null){
                pw.close();
            }
        }
        //删除之前的文件
        file.delete();
        String path = file.getAbsolutePath();
        wfile.renameTo(new File(path));
    }

    public static void main(String[] args) {
        HelloMojo mojo = new HelloMojo();
        mojo.suffix=".ftl";
        mojo.file = "C:\\Users\\38474\\Desktop\\jsoup\\jsoup\\jsoup\\src\\main";
        try {
            mojo.execute();
        } catch (MojoExecutionException e) {
            e.printStackTrace();
        } catch (MojoFailureException e) {
            e.printStackTrace();
        }
    }

}
