package de.mpa.client.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.RowFilter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import javax.swing.tree.TreeNode;

import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

public class SortableCheckBoxTreeTableNode extends CheckBoxTreeTableNode
		implements SortableTreeNode {

	private boolean sorted;
	private int[] modelToView;
	private Row[] viewToModel;

	public SortableCheckBoxTreeTableNode() {
		super();
	}

	public SortableCheckBoxTreeTableNode(Object... userObjects) {
		super(userObjects);
	}

	public SortableCheckBoxTreeTableNode(Object userObject, boolean fixed) {
		super(userObject, fixed);
	}

	@Override
	public int convertRowIndexToModel(int viewIndex) {
		return (sorted) ? viewToModel[viewIndex].modelIndex : viewIndex;
	}

	@Override
	public int convertRowIndexToView(int modelIndex) {
		return (!sorted || (modelIndex < 0) || (modelIndex >= modelToView.length)) ? modelIndex
				: modelToView[modelIndex];
	}

	@Override
	public boolean canSort() {
		return !isLeaf();
	}

	@Override
	public boolean canSort(List<? extends SortKey> sortKeys) {
		// TODO: re-enable this check after aggregate functions have been re-implemented
//		for (SortKey sortKey : sortKeys) {
//			if (!(getValueAt(sortKey.getColumn()) instanceof Comparable<?>)) {
//				return false;
//			}
//		}
		return true;
	}

	@Override
	public void reset() {
		sorted = false;
	}

	@Override
	public int getChildCount() {
		return (sorted) ? viewToModel.length : super.getChildCount();
	}
	
//	@Override
//	public void remove(int index) {
//		super.remove(index);
//		
//		Row[] viewToModel = new Row[this.viewToModel.length - 1];
//		System.arraycopy(this.viewToModel, 0, viewToModel, 0, index);
//		System.arraycopy(this.viewToModel, index + 1, viewToModel, index, this.viewToModel.length - index - 1);
//		this.viewToModel = viewToModel;
//		
//		int[] modelToView = new int[this.modelToView.length - 1];
//		System.arraycopy(this.modelToView, 0, modelToView, 0, index);
//		System.arraycopy(this.modelToView, index + 1, modelToView, index, this.modelToView.length - index - 1);
//		this.modelToView = modelToView;
//	}
	
	@Override
	public void removeAllChildren() {
		sorted = false;
		super.removeAllChildren();
	}
	
	@Override
	public boolean isLeaf() {
		return (children.size() == 0);
	}

	@Override
	public void sort(List<? extends SortKey> sortKeys,
			RowFilter<? super TableModel, ? super Integer> filter) {

		int childCount = children.size();
		int excludedCount = 0;
		
		// build view-to-model mapping
		modelToView = new int[childCount];
		List<Row> viewToModelList = new ArrayList<Row>(childCount);
		
		for (int i = 0; i < childCount; i++) {
			MutableTreeTableNode child = children.get(i);
			Row<TableModel, Integer> row = new Row<TableModel, Integer>(child, i, sortKeys);
			
			// check whether the child node is eligible for filtering (only
			// leaves can be actively filtered, parent nodes will automatically
			// become invisible when they have no visible children)
			if (child.isLeaf()) {
				// check whether the filter permits this child (if any filter is
				// configured at all)
				if ((filter == null) || (filter.include(row))) {
					viewToModelList.add(row);
				} else {
					excludedCount++;
				}
			} else {
				// check whether the child node has any visible children of its
				// own, if not treat as excluded
				if (child.getChildCount() > 0) {
					viewToModelList.add(row);
				} else {
					excludedCount++;
				}
			}
			
			// initialize model-to-view mapping while we're at it
			modelToView[i] = -1;
		}
		viewToModel = viewToModelList.toArray(new Row[childCount - excludedCount]);

		// sort view-to-model mapping
		if (sortKeys != null) {
			Arrays.sort(viewToModel);
		}
		
		// build model-to-view mapping
		for (int i = 0; i < viewToModel.length; i++) {
			modelToView[viewToModel[i].modelIndex] = i;
		}
		
		sorted = true;
	}

	@Override
	public TreeTableNode getChildAt(int childIndex) {
		if ((sorted) && (childIndex > viewToModel.length)) {
			return new SortableCheckBoxTreeTableNode("I SHOULD BE INVISIBLE");
		}
		return super.getChildAt(convertRowIndexToModel(childIndex));
	}

	@Override
	public int getIndex(TreeNode node) {
		return convertRowIndexToView(children.indexOf(node));
	}

	@Override
	public boolean isSorted() {
		return sorted;
	}
	
	@Override
	public void setParent(MutableTreeTableNode newParent) {
		super.setParent(newParent);
	}

	/**
	 * Provides a child node with the ability to be sorted and/or filtered.
	 */
	private class Row<M, I> extends RowFilter.Entry<M, I> implements Comparable<Row> {
		/**
		 * The tree table node containing the row's cell values.
		 */
		private TreeTableNode node;
		/**
		 * The row's model index.
		 */
		private int modelIndex;
		/**
		 * The row's list of column indices to be sorted and their respective
		 * sort orders.
		 */
		private List<? extends SortKey> sortKeys;

		/**
		 * Constructs a row object.
		 * @param node The node upon which comparisons are evaluated.
		 * @param modelIndex The row index.
		 * @param sortKeys The list of sort keys.
		 */
		public Row(TreeTableNode node, int modelIndex,
				List<? extends SortKey> sortKeys) {
			this.node = node;
			this.modelIndex = modelIndex;
			this.sortKeys = sortKeys;
		}

		@Override
		public String toString() {
			return ("" + this.modelIndex + " " + node.getUserObject()
					.toString());
		}

		@Override
		public M getModel() {
			return null; // we don't need this
		}

		@Override
		public int getValueCount() {
			return children.get(modelIndex).getColumnCount();
		}

		@Override
		public Object getValue(int index) {
			return children.get(modelIndex).getValueAt(index);
		}

		@Override
		public String getStringValue(int index) {
			Object value = children.get(modelIndex).getValueAt(index);
			return (value == null) ? "" : value.toString();
		}

		@Override
		public I getIdentifier() {
			return null; // we don't need this
		}

		@Override
		@SuppressWarnings("unchecked")
		public int compareTo(Row row) {
			// initialize result with fall-back value
			int result = this.modelIndex - row.modelIndex;
			for (SortKey sortKey : this.sortKeys) {
				if (sortKey.getSortOrder() != SortOrder.UNSORTED) {
					Object this_value = this.node.getValueAt(sortKey.getColumn());
					Object that_value = row.node.getValueAt(sortKey.getColumn());
					// define null as less than not-null
					if (this_value == null) {
						result = (that_value == null) ? 0 : -1;
					} else if (that_value == null) {
						result = 1;
					} else {
						// both value objects are not null, invoke compareTo()
						result = ((Comparable<Object>) this_value)
								.compareTo(that_value);
					}
					// correct result w.r.t. sort order
					if (sortKey.getSortOrder() == SortOrder.DESCENDING) {
						result *= -1;
					}
				}
				if (result != 0) {
					break;
				}
			}
			return result;
		}
		
	}
	
}