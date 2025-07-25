package jp.alhinc.calculate_sales;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NAME_SERIALNUMBER = "売上ファイル名が連番になっていません";
	private static final String FILE_DATA_OVER = "合計金額が10桁を超えました";
	private static final String FILE_DATA_NOCODE = "の支店コードが不正です";
	private static final String FILE_BAD_FORMAT = "のフォーマットが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		//コマンドライン引数が設定されているかどうか
		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店定義ファイル読み込み処理
		if (!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();

		//先にファイルの情報を格納するListを宣言する
		List<File> rcdFiles = new ArrayList<>();

		//getNameで売上集計課題フォルダ内のファイル名を取得する
		for (int i = 0; i < files.length; i++) {
			String fileName = files[i].getName();
			if (files[i].isFile() && fileName.matches("^[0-9]{8}[.]rcd$")) {
				rcdFiles.add(files[i]);
			}
		}
		//ソート
		Collections.sort(rcdFiles);

		for (int i = 0; i < rcdFiles.size() - 1; i++) {
			//比較する2つのファイル名の先頭から8文字切り出しint型に変換
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8));
			//2つのファイル名の数字の比較して、差が1ではなかったらエラーメッセージを表示
			if ((latter - former) != 1) {
				System.out.println(FILE_NAME_SERIALNUMBER);
				return;
			}
		}

		//rcdFilesに複数の売り上げファイル情報を格納してるため、その数だけ繰り返す
		for (int i = 0; i < rcdFiles.size(); i++) {
			BufferedReader br = null;

			try {
				File file = new File(args[0], rcdFiles.get(i).getName());
				FileReader fr = new FileReader(file);
				br = new BufferedReader(fr);

				String line;

				//リストの宣言
				List<String> fileData = new ArrayList<String>();

				//while文｛リストにadd｝
				while ((line = br.readLine()) != null) {
					fileData.add(line);
				}

				//売上ファイルが2行になってるかどうか
				if (fileData.size() != 2) {
					System.out.println(rcdFiles.get(i).getName() + FILE_BAD_FORMAT);
					return;
				}

				//売上ファイルの支店コードが支店定義ファイルに存在するか
				if (!branchSales.containsKey(fileData.get(0))) {
					System.out.println(rcdFiles.get(i).getName() + FILE_DATA_NOCODE);
					return;
				}

				//売上金額が数字かどうか
				if (!fileData.get(1).matches("^[0-9]*$")) {
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				//型変換
				long fileSale = Long.parseLong(fileData.get(1));

				//読み込んだ売り上げ金額を加算
				long saleAmount = branchSales.get(fileData.get(0)) + fileSale;

				//売上金額が11桁を超えた場合、エラーメッセージを表示する
				if (saleAmount >= 10000000000L) {
					System.out.println(FILE_DATA_OVER);
					return;
				}

				//Mapに追加
				branchSales.put(fileData.get(0), saleAmount);

			} catch (IOException e) {
				System.out.println(UNKNOWN_ERROR);
				return;
			} finally {
				// ファイルを開いている場合
				if (br != null) {
					try {
						// ファイルを閉じる
						br.close();
					} catch (IOException e) {
						System.out.println(UNKNOWN_ERROR);
						return;
					}
				}
			}
		}

		// 支店別集計ファイル書き込み処理
		if (!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}
	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			if (!file.exists()) {
				System.out.println(FILE_NOT_EXIST);
				return false;
			}
			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);

			String line;
			// 一行ずつ読み込む
			while ((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] item = line.split(",");
				//支店定義ファイルのフォーマットが正しいかどうか
				if ((item.length != 2) || (!item[0].matches("^[0-9]{3}"))) {
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}
				//Mapに支店コードと支店名を保持
				branchNames.put(item[0], item[1]);
				//Mapに支店コードと売り上げを保持
				branchSales.put(item[0], 0L);
			}

		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (br != null) {
				try {
					// ファイルを閉じる
					br.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames,
			Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)
		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);
			FileWriter fw = new FileWriter(file);
			bw = new BufferedWriter(fw);

			for (String key : branchNames.keySet()) {
				bw.write(key + "," + branchNames.get(key) + "," + branchSales.get(key));
				bw.newLine();
			}
		} catch (IOException e) {
			System.out.println(UNKNOWN_ERROR);
			return false;
		} finally {
			// ファイルを開いている場合
			if (bw != null) {
				try {
					// ファイルを閉じる
					bw.close();
				} catch (IOException e) {
					System.out.println(UNKNOWN_ERROR);
					return false;
				}
			}
		}

		return true;
	}

}
