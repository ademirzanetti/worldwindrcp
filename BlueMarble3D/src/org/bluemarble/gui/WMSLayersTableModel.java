package org.bluemarble.gui;

import java.util.ArrayList;
import org.fenggui.table.ITableModel;
import worldwind.contrib.parsers.WMS_Capabilities.Layer;

/**
 * Layers able model
 * @author Owner
 *
 */
class WMSLayersTableModel implements ITableModel
{
	String[][] matrix = null;
	private ArrayList<Layer> layers;
	
	public WMSLayersTableModel(ArrayList<Layer> layers)
	{
		matrix = new String[layers.size()][1];
		this.layers = layers;
	}

	public void update()
	{
		matrix = new String[layers.size()][1];
	}

	public String getColumnName(int columnIndex)
	{
		return "Column" + columnIndex;
	}

	public int getColumnCount()
	{
		return matrix[0].length;
	}

	public Object getValue(int row, int column)
	{
		if (matrix[row][column] == null)
		{
			return layers.get(row).Title;
		}
		return matrix[row][column];
	}

	public int getRowCount()
	{
		return matrix.length;
	}

	public void clear()
	{
		// TODO implement
	}

	public Object getValue(int row)
	{
		if (matrix[row] == null)
		{
			return layers.get(row).Title;
		}
		return matrix[row];
	}
}
