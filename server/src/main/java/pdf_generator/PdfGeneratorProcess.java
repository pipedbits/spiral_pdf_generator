package pdf_generator;

import java.lang.reflect.Method;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletOutputStream;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.AcroFields.FieldPosition;
import com.itextpdf.text.pdf.qrcode.*;
import com.itextpdf.text.html.*;

import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.*;
import org.apache.commons.fileupload.servlet.*;
import net.sf.json.JSONObject;
import net.sf.json.JSONArray;

/**
 * act�˱�����PDF�����
 */
public class PdfGeneratorProcess
{
	private HttpServletRequest m_req = null;
	private HttpServletResponse m_res = null;

	/**
     * �ᥤ������Υ���ץ�
     * @param _req HTTP�����֥�åȥꥯ������
     * @param _res HTTP�����֥�åȥ쥹�ݥ�
     */
	public void exec(HttpServletRequest _req,
					 HttpServletResponse _res)
    {
		m_req = _req;
		m_res = _res;

		String actionName  = null;
		String functionName = null;

		Class  aClass = null;
		Method actMethod = null;

		try {
			actionName = m_req.getParameter("_act");
			if (actionName == null || actionName.length() <= 0) {
				throw new Exception("no action.");
			}

			functionName = "act" + actionName;

			aClass = this.getClass();
			actMethod = aClass.getDeclaredMethod(functionName, null);
			actMethod.setAccessible(true);
			actMethod.invoke(this, null);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * PDFĢɼ
	 */
	private void actPdfReport()
	{
		try {
			DiskFileItemFactory itemFactory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(itemFactory);

			if (!ServletFileUpload.isMultipartContent(m_req)) {
				throw new Exception("E_NOT_MULTIPART");
			}

			List<FileItem> items = upload.parseRequest(m_req);
			int itemSize = items.size();

			FileItem item = null;
			String contentType = null;

			byte[] template = null;
			JSONObject json = null;

			// �ޥ���ѡ�����Υ���ƥ�Ĥ��˥����å�
			for (int i = 0; i < itemSize; i++) {
				item = items.get(i);

				// Content-Type
				contentType = item.getContentType();

				// name
				String name = item.getFieldName();

				if (name.equals("settings") && contentType.indexOf("application/json") > -1) {
					json = JSONObject.fromObject(item.getString("UTF-8"));
				// �ƥ�ץ졼��PDF�ξ��
				} else if (name.equals("template") && contentType.indexOf("application/pdf") > -1) {
					template = item.get();
				}
			}

			if (template == null) {
				throw new Exception("template is null.");
			}
			if (json == null) {
				throw new Exception("json is null.");
			}

			PdfReport report = new PdfReport(template, json);
			report.output(m_res);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
	}

	/**
	 * PDF���ɤ����Υ����å�
	 */
	private void actPdfCheck()
	{
		ServletOutputStream objOut = null;

		try {
			DiskFileItemFactory itemFactory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(itemFactory);

			if (!ServletFileUpload.isMultipartContent(m_req)) {
				throw new Exception("E_NOT_MULTIPART");
			}

			List<FileItem> items = upload.parseRequest(m_req);
			int itemSize = items.size();

			FileItem item = null;
			String contentType = null;

			byte[] template = null;

			// �ޥ���ѡ�����Υ���ƥ�Ĥ��˥����å�
			for (int i = 0; i < itemSize; i++) {
				item = items.get(i);

				// Content-Type
				contentType = item.getContentType();

				// name
				String name = item.getFieldName();

				if (name.equals("template") && contentType.indexOf("application/pdf") > -1) {
					template = item.get();
				}
			}

			if (template == null) {
				throw new Exception("template is null.");
			}

			String result = Integer.toString(PdfUtil.checkPdf(template));

			// ��̤򥯥饤����Ȥ˽���
			byte[] buf = result.getBytes();
			m_res.setContentType("text/html");
			m_res.setContentLength(buf.length);

			objOut = m_res.getOutputStream();
			objOut.write(buf);
			objOut.flush();
			objOut.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (objOut != null) {
				try {
					objOut.close();
				} catch (IOException e) {
					/* ignore */
				}
				objOut = null;
			}
		}
    }
}

