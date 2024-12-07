package pdf_generator;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ưŪPDF���ϥ����֥�å�
 */
public class PdfGenerator extends HttpServlet
{
    /**
     * �����֥�åɥ��饹���ɻ��ν���
	 *
	 * @param _config ServletConfig���֥�������
     */
    public void init(ServletConfig _config)
    {
        try {
            super.init(_config);

        } catch (Exception e) {
			e.printStackTrace();

        } finally {
            // �����ʤ�
        }
    }

	/**
	 * GET���ν���
	 *
	 * @param _req HTTP�����֥�åȥꥯ������
	 * @param _res HTTP�����֥�åȥ쥹�ݥ�
	 */
	public void doGet(HttpServletRequest _req,
					  HttpServletResponse _res)
	{
		exec(_req, _res);
	}

	/**
	 * POST���ν�����Ԥ��ޤ���
	 *
	 * @param _req HTTP�����֥�åȥꥯ������
	 * @param _res HTTP�����֥�åȥ쥹�ݥ�
	 */
	public void doPost(HttpServletRequest _req,
					   HttpServletResponse _res)
	{
		exec(_req, _res);
	}

	/**
	 * �ᥤ�����
	 *
	 * @param _req HTTP�����֥�åȥꥯ������
	 * @param _res HTTP�����֥�åȥ쥹�ݥ�
	 */
	private void exec(HttpServletRequest _req,
					  HttpServletResponse _res)
	{
		PdfGeneratorProcess proc = new PdfGeneratorProcess();
		proc.exec(_req, _res);
	}
}
