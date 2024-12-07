package spiral;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;

import okhttp3.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

/**
 * SPIRAL PDF Generatorサーバへリクエストするサンプルのクライアントプログラム.
 */
public class SampleServlet extends HttpServlet
{
  private static final String PDF_SERVER_URL = "http://127.0.0.1:8080/spiral_pdf_generator/PdfGenerator?_act=PdfReport";

  // 暗号化方式
  //private static final int ENC_RC4_40     = 1;    // 40-bit RC4
  //private static final int ENC_RC4_128    = 2;    // 128-bit RC4
  //private static final int ENC_AES_128    = 3;    // 128-bit AES
  //private static final int ENC_AES_256    = 4;    // 256-bit AES
  //private static final short DEFAULT_ENCRYPTION   = ENC_RC4_128;

  // PDFのセキュリティ
  public static final int ALLOW_SCREENREADERS      = 0x00000001;// アクセシビリティのための内容抽出
  public static final int ALLOW_COPY               = 0x00000010;// 内容のコピー
  public static final int ALLOW_PRINTING           = 0x00000100;// 印刷
  public static final int ALLOW_ASSEMBLY           = 0x00001000;// 文書アセンブリ
  public static final int ALLOW_DEGRADED_PRINTING  = 0x00010000;// 低品質印刷
  public static final int ALLOW_FILL_IN            = 0x00100000;// フォームフィールドの入力と署名
  public static final int ALLOW_MODIFY_CONTENTS    = 0x01000000;// 注釈の入力
  public static final int ALLOW_MODIFY_ANNOTATIONS = 0x10000000;// 文書の変更

  // データ出力形式
  private static final int OUTPUT_VALUE   = 1;    // 値
  //private static final int OUTPUT_IMAGE   = 2;    // 画像
  //private static final int OUTPUT_QR      = 3;    // QRコード

  /**
   * PDFをアップロードする画面を表示.
   * @param _req HttpServletRequest
   * @param _res HttpServletResponse
   * @throws IOException IOException
   */
  public void doGet (HttpServletRequest _req, HttpServletResponse _res) throws IOException
  {
    try (OutputStreamWriter out = new OutputStreamWriter(_res.getOutputStream(), StandardCharsets.UTF_8)) {
      out.write("<!DOCTYPE html>\n");
      out.write("<html lang='ja'>\n");
      out.write("<head>\n");
      out.write("<meta charset='UTF-8'>\n");
      out.write("</head>\n");
      out.write("<body>\n");
      out.write("<h1>SPIRAL PDF Sample</h1>\n");

      out.write("<h3>FieldName and Field Value</h3>\n");
      out.write("<form method='post' action='/spiral_pdf_sample/Sample' enctype='multipart/form-data'>\n");

      out.write("Field01: \n");
      out.write("name: <input type='text' size='20' name='key01' value='email' disabled>\n");
      out.write("value: <input type='text' size='40' name='val01' value='mailaddress@example.com' disabled><br><br>\n");

      out.write("Field02: \n");
      out.write("name: <input type='text' size='20' name='key02' value='name' disabled>\n");
      out.write("value: <input type='text' size='40' name='val02' value='テスト太郎' disabled><br><br\n>");

      out.write("<h3>PDF Template</h3>\n");
      out.write("Template: \n");
      out.write("<input type='file' name='template'><br><br><br>\n");

      out.write("<input type='submit' value='download'>\n");
      out.write("</form>\n");
      out.write("</body>\n");
      out.write("</html>\n");
    }
  }

  /**
   * PDFテンプレートをアップロードしてPDF Generatorサーバへ送信.
   * @param _req HttpServletRequest
   * @param _res HttpServletResponse
   * @throws IOException IOException
   */
  public void doPost (HttpServletRequest _req, HttpServletResponse _res) throws IOException
  {
    // PDFテンプレートを受領
    ByteArrayOutputStream pdfTemplate = new ByteArrayOutputStream();
    parseMultipart(_req, pdfTemplate);

    // sample data
    JSONObject settingJson = createSampleData();

    // SEND
    byte[] buf = send(settingJson.toString(), pdfTemplate.toByteArray());

    if (buf == null) {
      throw new IOException("PDF contents is null.");
    }

    // OUTPUT PDF Header
    _res.setContentType("application/pdf");
    _res.setHeader("Content-disposition", "attachment; filename=\"" + "sample.pdf" + "\"");
    _res.setContentLength(buf.length);

    // OUTPUT PDF Body
    try (ServletOutputStream out = _res.getOutputStream()) {
      out.write(buf);
      out.flush();
    }
  }

  /**
   * PDF Generatorサーバへ送信するサンプルデータを作成.
   * @return JSONObject
   */
  private JSONObject createSampleData()
  {
    // Prepare the key and values JSON
    // {
    //   "fileName":"",
    //   "ownPass":"",
    //   "usrPass":"",
    //   "permission":"286331153",
    //   "encryption":"1",
    //   "dataList": [
    //                { "target_name":"email", "value":"mailaddress@example.com", "output_type":1 },
    //                { "target_name":"name" , "value":"テスト太郎"                , "output_type":1 }
    //               ]
    // }
    List<String> dataList = new ArrayList<>();

    JSONObject data01 = new JSONObject();
    data01.put("target_name", "email");
    data01.put("value"      , "mailaddress@example.com");
    data01.put("output_type", OUTPUT_VALUE);
    dataList.add(data01.toString());

    JSONObject data02 = new JSONObject();
    data02.put("target_name", "name");
    data02.put("value"      , "テスト太郎");
    data02.put("output_type", OUTPUT_VALUE);
    dataList.add(data02.toString());

    // Option
    int permissions = 0;
    permissions |= ALLOW_SCREENREADERS;
    permissions |= ALLOW_COPY;
    permissions |= ALLOW_PRINTING;
    permissions |= ALLOW_ASSEMBLY;
    permissions |= ALLOW_DEGRADED_PRINTING;
    permissions |= ALLOW_FILL_IN;
    permissions |= ALLOW_MODIFY_CONTENTS;
    permissions |= ALLOW_MODIFY_ANNOTATIONS;

    JSONObject settingJson = new JSONObject();
    settingJson.put("fileName", "");
    settingJson.put("ownPass" , "");
    settingJson.put("usrPass" , "");
    settingJson.put("permission", Integer.toString(permissions));
    settingJson.put("encryption", Integer.toString(1));

    JSONArray jsonKeyVals = JSONArray.fromObject(dataList);
    settingJson.put("dataList", jsonKeyVals.toString());

    System.out.println("DEBUG: " + settingJson);

    return settingJson;
  }

  /**
   * PDFテンプレート（template）と、差し替えキーワード設定（settings）をマルチパートリクエストでPDFサーバへ送信.
   *
   * @param _settingsJson JSON for configuration to specify optional functions.
   * @param _pdfTemplate  PDF Template Binary.
   * @return PDF file of processing results.
   * @throws IOException IOException
   */
  private byte[] send(String _settingsJson, byte[] _pdfTemplate) throws IOException
  {
    RequestBody jsonBody = RequestBody.create(MediaType.get("application/json"), _settingsJson);
    RequestBody fileBody = RequestBody.create(MediaType.get("application/pdf"), _pdfTemplate);

    MultipartBody multipartBody = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("template", "filename", fileBody)
            .addFormDataPart("settings", null, jsonBody)
            .build();

    Request request = new Request.Builder()
            .url(SampleServlet.PDF_SERVER_URL)
            .post(multipartBody)
            .build();

    OkHttpClient okHttpClient = new OkHttpClient()
            .newBuilder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .build();

    try (Response resp = okHttpClient.newCall(request).execute()) {
      return resp.body() != null ? resp.body().bytes() : null;
    }
  }

  /**
   * サンプル画面からアップロードしたPDFテンプレートをパース.
   */
  private void parseMultipart(HttpServletRequest _req, ByteArrayOutputStream _pdfTemplate) throws IOException
  {
    DiskFileItemFactory itemFactory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(itemFactory);
    List<FileItem> items;

    if (!ServletFileUpload.isMultipartContent(_req)) {
      throw new IOException("NOT MULTIPART");
    }

    // parse
    try {
      items = upload.parseRequest(_req);
    } catch (Exception e) {
      throw new IOException(e);
    }

    // check the contents
    for (FileItem item : items) {
        String contentType = item.getContentType();
        String name = item.getFieldName();

        if (name.equals("template") && contentType.contains("application/pdf")) {
            byte[] pdfTemplate = item.get();
            _pdfTemplate.write(pdfTemplate, 0, pdfTemplate.length);
        }
      }

    if (_pdfTemplate.size() == 0) {
      throw new IOException("pdf template is null.");
    }
  }
}
