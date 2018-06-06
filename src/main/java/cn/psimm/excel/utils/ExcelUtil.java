package cn.psimm.excel.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * String excel 工具类
 * 
 * @author sonta
 *
 */
@SuppressWarnings("deprecation")
public class ExcelUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(ExcelUtil.class);

	/**
	 * 获取指定sheet、行、列开始的数据集合
	 * 
	 * @param path
	 *            excel 路径
	 * @param headAttrs
	 *            头部字段集合
	 * @param sheetIndex
	 * @param rowBegin
	 * @param columnBegin
	 * @param headerRowIndex
	 *            头部字段所在行号 会根据以头部字段为key的list map
	 * @return
	 * @throws Exception
	 */
	public static List<Map<String, String>> getListMapByHeader(String path,
			String[] headerAttrs, int sheetIndex, int rowBegin,
			int columnBegin, int headerRowIndex) throws Exception {

		List<Map<String, String>> ret = new ArrayList<Map<String, String>>();
		Workbook wb = null;
		InputStream inp = null;
		try {
			inp = new FileInputStream(path);
			wb = WorkbookFactory.create(inp);
		} catch (FileNotFoundException e) {
			logger.error("get sheet rows line data gather error -path:{} - error message:", path, e);
		} catch (InvalidFormatException e) {
			logger.error("get sheet rows line data gather error -path:{} - error message:", path, e);
		} catch (IOException e) {
			logger.error("get sheet rows line data gather error -path:{} - error message:", path, e);
		}
		if (wb == null) {
			return null;
		}
		Sheet sheet = wb.getSheetAt(sheetIndex);
		Map<Integer, String> columnIndex = new HashMap<Integer, String>();
		int columnCount = headerAttrs.length;
		Row headerRow = sheet.getRow(headerRowIndex);
		if (headerRow == null) {
			throw new Exception("获取指定行号的header 失败");
		}
		// 将header根据实际排序方式封装在map中
		for (int i = columnBegin; i < columnBegin + columnCount; i++) {
			Cell cell = headerRow.getCell(i, Row.CREATE_NULL_AS_BLANK);
			String cellValueStr = cell.getStringCellValue();
			if (!isHeaderCellExist(headerAttrs, cellValueStr)) {
				throw new Exception("指定的header["
						+ JSONArray.fromObject(headerAttrs) + "]不存在值["
						+ cellValueStr + "]");
			}
			columnIndex.put(i, cellValueStr);
		}
		int rowNum = 0;
		for (Row row : sheet) {
			if (rowNum < rowBegin) {
				rowNum++;
				continue;
			}
			Map<String, String> rowMap = new HashMap<String, String>();
			for (int i = columnBegin; i < columnBegin + columnCount; i++) {
				Cell cell = row.getCell(i, Row.CREATE_NULL_AS_BLANK);
				int cellType = cell.getCellType();
				switch (cellType) {
				case Cell.CELL_TYPE_BLANK:
					rowMap.put(columnIndex.get(i), "");
					break;
				case Cell.CELL_TYPE_BOOLEAN:
					rowMap.put(columnIndex.get(i),
							String.valueOf(cell.getBooleanCellValue()));
					break;
				// 数值
				case Cell.CELL_TYPE_NUMERIC:
					if (DateUtil.isCellDateFormatted(cell)) {
						rowMap.put(columnIndex.get(i),
								String.valueOf(cell.getDateCellValue()));
					} else {
						cell.setCellType(Cell.CELL_TYPE_STRING);
						String temp = cell.getStringCellValue();
						// 判断是否包含小数点，如果不含小数点，则以字符串读取，如果含小数点，则转换为Double类型的字符串
						if (temp.indexOf(".") > -1) {
							rowMap.put(columnIndex.get(i),
									new Double(temp).toString());
						} else {
							rowMap.put(columnIndex.get(i), temp.trim());
						}
					}
					break;
				case Cell.CELL_TYPE_STRING:
					rowMap.put(columnIndex.get(i), cell.getStringCellValue()
							.trim());
					break;
				case Cell.CELL_TYPE_ERROR:
					rowMap.put(columnIndex.get(i), "");
					break;
				case Cell.CELL_TYPE_FORMULA:
					cell.setCellType(Cell.CELL_TYPE_STRING);
					String temp = cell.getStringCellValue();
					if (temp != null) {
						rowMap.put(columnIndex.get(i),
								temp.replaceAll("#N/A", "").trim());
					}
					break;
				default:
					rowMap.put(columnIndex.get(i), "");
					break;
				}
			}
			ret.add(rowMap);
		}
		return ret;
	}

	/**
	 * 获取excel中有效数据行数
	 * 
	 * @param path
	 * @param headerAttrs
	 * @param sheetIndex
	 * @param rowBegin
	 * @param columnBegin
	 * @param headerRowIndex
	 * @return
	 * @throws Exception
	 */
	public static int getAvailableRows(String path, int sheetIndex, int rowBegin)
			throws Exception {

		Workbook wb = null;
		InputStream inp = null;
		try {
			inp = new FileInputStream(path);
			wb = WorkbookFactory.create(inp);
		} catch (FileNotFoundException e) {
			logger.error("get excel valid data rows error -path:{} - error message:", path, e);
		} catch (InvalidFormatException e) {
			logger.error("get excel valid data rows error -path:{} - error message:", path, e);
		} catch (IOException e) {
			logger.error("get excel valid data rows error -path:{} - error message:", path, e);
		}
		if (wb == null) {
			return 0;
		}
		Sheet sheet = wb.getSheetAt(sheetIndex);
		return sheet.getLastRowNum() - rowBegin + 2;
	}

	/**
	 * 将指定下载数据存入生成的文件中
	 * 
	 * @author ppj 2016年10月25日14:11:36
	 * @param path
	 *            文件存放路径
	 * @param fileName 
	 * 			    文件名称
	 * @param title
	 *            标题
	 * @param secTitle
	 *            标题2
	 * @param header
	 *            表头
	 * @param keys
	 * 			  用于生成map遍历键值对
	 * @param data
	 *            List<Map<String, Object>> list集合代表全部要插入的数据 ,每一个Map
	 *            代表excel表中的一行数据 Map中key的值对应的是表头(hade)中的字符串
	 *            Value则对应表头(hade)列中插入的值 请务必保证 hade中的元素个数 与 Map中的Key-Value对数量一致.
	 * @return 成功则返回 创建该文件的文件名 失败则抛出异常
	 * @throws Exception
	 */
	public static void setListMapToExcel(String path, String fileName, String title, String secTitle,
			String[] header, String[] keys, List<Map<String, Object>> data){
		// 开始准备生成表格
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet sheet = wb.createSheet(title);
		for (int i = 0; i < header.length; i++) {
			sheet.setColumnWidth(i, 30 * 256);
		}
		// 设置表格样式
		HSSFCellStyle style = wb.createCellStyle();
		style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
		HSSFFont fontSearch = wb.createFont();
		fontSearch.setFontHeightInPoints((short) 16);
		style.setFont(fontSearch);

		HSSFCellStyle style2 = wb.createCellStyle();
		style2.setAlignment(HSSFCellStyle.ALIGN_RIGHT);
		HSSFFont fontSearch2 = wb.createFont();
		fontSearch2.setFontHeightInPoints((short) 13);
		style2.setFont(fontSearch2);

		HSSFCellStyle style3 = wb.createCellStyle();
		style3.setAlignment(HSSFCellStyle.ALIGN_LEFT);
		HSSFFont fontSearch3 = wb.createFont();
		fontSearch3.setFontHeightInPoints((short) 10);
		style3.setFont(fontSearch3);
		// 开始创建表格
		// 标题
		HSSFRow rowHeard = sheet.createRow(0);
		HSSFCell cellHeard = rowHeard.createCell(0);
		cellHeard.setCellValue(title);
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, header.length - 1));// (起始行,结束行,起始列,结束列)
		cellHeard.setCellStyle(style);
		// 副标题
		HSSFRow rowHeard2 = sheet.createRow(1);
		HSSFCell cellHeard2 = rowHeard2.createCell(0);
		cellHeard2.setCellValue(secTitle);
		sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, header.length - 1));
		cellHeard2.setCellStyle(style2);
		// 表头
		HSSFRow rowHeard3 = sheet.createRow(2);
		for (int i = 0; i < header.length; i++) {
			HSSFCell cellHeard3 = rowHeard3.createCell(i);
			cellHeard3.setCellValue(header[i]);
			cellHeard3.setCellStyle(style3);
		}
		// 开始存入数据
		for (int i = 0; i < data.size(); i++) {
			rowHeard3 = sheet.createRow(3 + i);
			Map<String, Object> map = data.get(i);
			for(int j = 0; j < keys.length; j++){
				HSSFCell cellHeard3 = rowHeard3.createCell(j);
				cellHeard3.setCellValue(""+map.get(keys[j]));
				cellHeard3.setCellStyle(style3);
			}
		}
		// 生成文件地址
		String excelFileName = fileName + "." + "xls";
		String excelPath = path + File.separator + excelFileName;
		// 开始生成文件
		File excelFile = new File(excelPath);
		if (!excelFile.getParentFile().exists()) {
			excelFile.getParentFile().mkdirs();
		}
		// 将数据写入文件
		FileOutputStream fileOut;
		try {
			fileOut = new FileOutputStream(excelPath);
			wb.write(fileOut);
			fileOut.flush();
			fileOut.close();
		} catch (FileNotFoundException e) {
			logger.error("get valid data create excel error -path:{} - error message:", excelPath, e);
		} catch (IOException e) {
			logger.error("get valid data create excel error -path:{} - error message:", excelPath, e);
		}
	}

	/**
	 * 判断header中是否存在当前读取的key
	 * 
	 * @param headerAttrs
	 * @param cellStr
	 * @return
	 */
	private static boolean isHeaderCellExist(String[] headerAttrs,
			String cellStr) {
		for (String key : headerAttrs) {
			if (key.equals(cellStr)) {
				return true;
			}
		}
		return false;
	}
	
	
	public static void main(String[] args) {
		
	}
}
