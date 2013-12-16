package org.openlca.core.results;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.openlca.core.matrix.LongPair;
import org.openlca.core.matrix.ProductIndex;
import org.openlca.core.model.descriptors.BaseDescriptor;
import org.openlca.core.model.descriptors.FlowDescriptor;
import org.openlca.core.model.descriptors.ImpactCategoryDescriptor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Calculates a contribution tree of the processes in a product system to a flow
 * or impact assessment result.
 */
class ContributionTreeCalculator {

	private AnalysisResult result;
	private LinkContributions linkContributions;
	private Multimap<LongPair, LongPair> links;
	private boolean skipNegatives = false;
	private boolean skipNulls = false;

	public ContributionTreeCalculator(AnalysisResult result,
			LinkContributions linkContributions) {
		this.result = result;
		this.linkContributions = linkContributions;
		this.links = makeLinks(result.getProductIndex());
	}

	private Multimap<LongPair, LongPair> makeLinks(ProductIndex index) {
		Multimap<LongPair, LongPair> links = ArrayListMultimap.create();
		for (LongPair input : index.getLinkedInputs()) {
			long recipientProcess = input.getFirst();
			LongPair provider = index.getLinkedOutput(input);
			for (LongPair recipient : index.getProducts(recipientProcess))
				links.put(recipient, provider);
		}
		return links;
	}

	public void skipNegativeValues(boolean skipNegatives) {
		this.skipNegatives = skipNegatives;
	}

	public void skipNullValues(boolean skipNulls) {
		this.skipNulls = skipNulls;
	}

	public ContributionTree calculate(FlowDescriptor flow) {
		FlowResultFetch fn = new FlowResultFetch(flow);
		return calculate(fn);
	}

	public ContributionTree calculate(ImpactCategoryDescriptor impact) {
		ImpactResultFetch fn = new ImpactResultFetch(impact);
		return calculate(fn);
	}

	private ContributionTree calculate(ResultFetch fn) {

		ContributionTree tree = new ContributionTree();
		tree.setReference(fn.getReference());
		ContributionTreeNode root = new ContributionTreeNode();
		LongPair refProduct = result.getProductIndex().getRefProduct();
		root.setShare(1d);
		root.setProcessProduct(refProduct);
		root.setAmount(fn.getTotalAmount(refProduct));
		tree.setRoot(root);

		NodeSorter sorter = new NodeSorter();
		Stack<ContributionTreeNode> stack = new Stack<>();
		stack.push(root);
		HashSet<LongPair> handled = new HashSet<>();
		handled.add(refProduct);

		while (!stack.isEmpty()) {

			ContributionTreeNode node = stack.pop();
			List<ContributionTreeNode> childs = createChildNodes(node, fn);
			Collections.sort(childs, sorter);
			node.getChildren().addAll(childs);

			for (int i = childs.size() - 1; i >= 0; i--) {
				// push in reverse order, so that the highest contribution is
				// on the top
				ContributionTreeNode child = childs.get(i);
				if (!handled.contains(child.getProcessProduct())) {
					stack.push(child);
					handled.add(child.getProcessProduct());
				}
			}
		}
		return tree;
	}

	private List<ContributionTreeNode> createChildNodes(
			ContributionTreeNode parent, ResultFetch fn) {
		List<ContributionTreeNode> childNodes = new ArrayList<>();
		LongPair recipient = parent.getProcessProduct();
		for (LongPair provider : links.get(recipient)) {
			double share = linkContributions.getShare(provider, recipient)
					* parent.getShare();
			double amount = share * fn.getTotalAmount(provider);
			if (amount == 0 && skipNulls)
				continue;
			if (amount < 0 && skipNegatives)
				continue;
			ContributionTreeNode node = new ContributionTreeNode();
			node.setShare(share);
			node.setAmount(amount);
			node.setProcessProduct(provider);
			childNodes.add(node);
		}
		return childNodes;
	}

	private interface ResultFetch {

		double getTotalAmount(LongPair product);

		BaseDescriptor getReference();
	}

	private class FlowResultFetch implements ResultFetch {

		private final FlowDescriptor flow;
		private final long flowId;

		public FlowResultFetch(FlowDescriptor flow) {
			this.flow = flow;
			this.flowId = flow.getId();
		}

		@Override
		public double getTotalAmount(LongPair product) {
			return result.getTotalFlowResult(product, flowId);
		}

		@Override
		public BaseDescriptor getReference() {
			return flow;
		}
	}

	private class ImpactResultFetch implements ResultFetch {

		private final ImpactCategoryDescriptor impact;
		private final long impactId;

		public ImpactResultFetch(ImpactCategoryDescriptor impact) {
			this.impact = impact;
			this.impactId = impact.getId();
		}

		@Override
		public double getTotalAmount(LongPair processProduct) {
			return result.getTotalImpactResult(processProduct, impactId);
		}

		@Override
		public BaseDescriptor getReference() {
			return impact;
		}
	}

	private class NodeSorter implements Comparator<ContributionTreeNode> {
		@Override
		public int compare(ContributionTreeNode node1,
				ContributionTreeNode node2) {
			return Double.compare(node2.getAmount(), node1.getAmount());
		}
	}

}
