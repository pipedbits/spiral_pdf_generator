package pdf_generator;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 動的PDF出力サーブレット
 */
public class PdfGenerator extends HttpServlet
{
    /**
     * サーブレッドクラスロード時の処理
	 *
	 * @param _config ServletConfigオブジェクト
     */
    public void init(ServletConfig _config)
    {
        try {
            super.init(_config);

        } catch (Exception e) {
			e.printStackTrace();

        } finally {
            // 処理なし
        }
    }

	/**
	 * GET時の処理
	 *
	 * @param _req HTTPサーブレットリクエスト
	 * @param _res HTTPサーブレットレスポンス
	 */
	public void doGet(HttpServletRequest _req,
					  HttpServletResponse _res)
	{
		exec(_req, _res);
	}

	/**
	 * POST時の処理を行います。
	 *
	 * @param _req HTTPサーブレットリクエスト
	 * @param _res HTTPサーブレットレスポンス
	 */
	public void doPost(HttpServletRequest _req,
					   HttpServletResponse _res)
	{
		exec(_req, _res);
	}

	/**
	 * メイン処理
	 *
	 * @param _req HTTPサーブレットリクエスト
	 * @param _res HTTPサーブレットレスポンス
	 */
	private void exec(HttpServletRequest _req,
					  HttpServletResponse _res)
	{
		PdfGeneratorProcess proc = new PdfGeneratorProcess();
		proc.exec(_req, _res);
	}
}
