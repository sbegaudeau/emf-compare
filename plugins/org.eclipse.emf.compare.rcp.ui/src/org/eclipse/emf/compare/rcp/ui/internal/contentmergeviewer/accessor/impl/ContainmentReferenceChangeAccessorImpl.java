/*******************************************************************************
 * Copyright (c) 2012, 2013 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.emf.compare.rcp.ui.internal.contentmergeviewer.accessor.impl;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.getFirst;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.containmentReferenceChange;

import com.google.common.collect.ImmutableList;

import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceState;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.ResourceAttachmentChange;
import org.eclipse.emf.compare.rcp.ui.internal.contentmergeviewer.impl.TypeConstants;
import org.eclipse.emf.compare.rcp.ui.internal.mergeviewer.IMergeViewer.MergeViewerSide;
import org.eclipse.emf.compare.rcp.ui.internal.mergeviewer.item.IMergeViewerItem;
import org.eclipse.emf.compare.rcp.ui.internal.mergeviewer.item.impl.MergeViewerItem;
import org.eclipse.emf.compare.rcp.ui.internal.mergeviewer.item.impl.MergeViewerItem.Container;
import org.eclipse.emf.compare.rcp.ui.internal.util.MergeViewerUtil;
import org.eclipse.emf.ecore.EObject;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class ContainmentReferenceChangeAccessorImpl extends AbstractStructuralFeatureAccessor {
	/**
	 * 
	 */
	public ContainmentReferenceChangeAccessorImpl(AdapterFactory adapterFactory,
			ReferenceChange referenceChange, MergeViewerSide side) {
		super(adapterFactory, referenceChange, side);
	}

	/**
	 * @return
	 */
	@Override
	protected ImmutableList<Diff> computeDifferences() {
		List<Diff> allDifferences = getComparison().getDifferences();
		return ImmutableList.<Diff> copyOf(filter(allDifferences, containmentReferenceChange()));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.mergeviewer.accessor.ICompareAccessor#getInitialItem()
	 */
	@Override
	public IMergeViewerItem getInitialItem() {
		Diff initialDiff = getInitialDiff();
		EObject diffValue = (EObject)MergeViewerUtil.getDiffValue(initialDiff);
		Match match = getComparison().getMatch(diffValue);

		if (match == null && DifferenceState.MERGED == initialDiff.getState()
				&& MergeViewerSide.ANCESTOR != getSide()) {
			match = getMatchWithNullValues(initialDiff.getMatch());
		}
		if (match != null) {
			return new MergeViewerItem.Container(getComparison(), getInitialDiff(), match, getSide(),
					getRootAdapterFactory());
		}
		return null;
	}

	/**
	 * After merging a diff which will lead to have an insertion point on both sides, the match associated
	 * with this diff will be unreacheable because its left and right sides will be null. This method will
	 * find this match.
	 * 
	 * @param match
	 * @return the match associated with the given merged diff.
	 */
	private Match getMatchWithNullValues(Match match) {
		for (Match subMatch : match.getSubmatches()) {
			if (subMatch.getLeft() == null && subMatch.getRight() == null) {
				return subMatch;
			}
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.mergeviewer.accessor.ICompareAccessor#getItems()
	 */
	public ImmutableList<? extends IMergeViewerItem> getItems() {
		final ImmutableList.Builder<IMergeViewerItem> ret = ImmutableList.builder();

		EList<Match> matches = getComparison().getMatches();
		for (Match match : matches) {
			ResourceAttachmentChange diff = getFirst(filter(match.getDifferences(),
					ResourceAttachmentChange.class), null);
			Container container = null;
			if (getSide() != MergeViewerSide.ANCESTOR
					|| (getSide() == MergeViewerSide.ANCESTOR && match.getOrigin() != null)) {
				container = new MergeViewerItem.Container(getComparison(), diff, match.getLeft(), match
						.getRight(), match.getOrigin(), getSide(), getRootAdapterFactory());
				ret.add(container);
			}
		}

		return ret.build();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.rcp.ui.internal.contentmergeviewer.accessor.legacy.ITypedElement#getType()
	 */
	@Override
	public String getType() {
		return TypeConstants.TYPE__ETREE_DIFF;
	}
}
