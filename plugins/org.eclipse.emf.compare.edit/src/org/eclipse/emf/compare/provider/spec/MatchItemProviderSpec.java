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
package org.eclipse.emf.compare.provider.spec;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Iterables.any;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.transform;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.containmentReferenceChange;
import static org.eclipse.emf.compare.utils.EMFComparePredicates.hasState;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import java.util.Collection;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.DifferenceState;
import org.eclipse.emf.compare.Match;
import org.eclipse.emf.compare.ReferenceChange;
import org.eclipse.emf.compare.ResourceAttachmentChange;
import org.eclipse.emf.compare.provider.AdapterFactoryUtil;
import org.eclipse.emf.compare.provider.IItemDescriptionProvider;
import org.eclipse.emf.compare.provider.IItemStyledLabelProvider;
import org.eclipse.emf.compare.provider.MatchItemProvider;
import org.eclipse.emf.compare.provider.utils.ComposedStyledString;
import org.eclipse.emf.compare.provider.utils.IStyledString;
import org.eclipse.emf.compare.provider.utils.IStyledString.Style;
import org.eclipse.emf.ecore.EObject;

/**
 * Specialized {@link MatchItemProvider} returning nice output for {@link #getText(Object)} and
 * {@link #getImage(Object)}.
 * 
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 */
public class MatchItemProviderSpec extends MatchItemProvider implements IItemStyledLabelProvider, IItemDescriptionProvider {
	/** The image provider used with this item provider. */
	private final OverlayImageProvider overlayProvider;

	/**
	 * Constructor calling super {@link #MatchItemProvider(AdapterFactory)}.
	 * 
	 * @param adapterFactory
	 *            the adapter factory
	 */
	public MatchItemProviderSpec(AdapterFactory adapterFactory) {
		super(adapterFactory);
		overlayProvider = new OverlayImageProvider(getResourceLocator());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.provider.MatchItemProvider#getImage(java.lang.Object)
	 */
	@Override
	public Object getImage(Object object) {
		Match match = (Match)object;
		Object ret = AdapterFactoryUtil.getImage(getRootAdapterFactory(), match.getLeft());

		if (ret == null) {
			ret = AdapterFactoryUtil.getImage(getRootAdapterFactory(), match.getRight());
		}

		if (ret == null) {
			ret = AdapterFactoryUtil.getImage(getRootAdapterFactory(), match.getOrigin());
		}

		if (ret == null) {
			ret = super.getImage(object);
		}

		Object matchImage = overlayProvider.getComposedImage(match, ret);
		ret = overlayImage(object, matchImage);

		return ret;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.provider.MatchItemProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object object) {
		Match match = (Match)object;
		String ret = AdapterFactoryUtil.getText(getRootAdapterFactory(), match.getLeft());

		if (ret == null) {
			ret = AdapterFactoryUtil.getText(getRootAdapterFactory(), match.getRight());
		}

		if (ret == null) {
			ret = AdapterFactoryUtil.getText(getRootAdapterFactory(), match.getOrigin());
		}

		if (ret == null) {
			ret = super.getText(object);
		}

		return ret;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.edit.provider.ItemProviderAdapter#getChildren(java.lang.Object)
	 */
	@Override
	public Collection<?> getChildren(Object object) {
		Match match = (Match)object;
		Iterable<?> filteredChildren = getChildrenIterable(match);
		return ImmutableList.copyOf(filteredChildren);
	}

	/**
	 * Returns the children that will be displayed under the given Match.
	 * 
	 * @param match
	 *            the given Match.
	 * @return an iterable of children that will be displayed under the given Match.
	 * @since 3.0
	 */
	public Iterable<?> getChildrenIterable(Match match) {
		ImmutableSet<EObject> containementDifferenceValues = containmentReferencesValues(match);

		@SuppressWarnings("unchecked")
		Predicate<Object> childrenFilter = not(or(matchOfContainmentDiff(containementDifferenceValues),
				emptyMatch(), instanceOf(ResourceAttachmentChange.class)));

		Iterable<?> filteredChildren = filter(super.getChildren(match), childrenFilter);
		return filteredChildren;
	}

	/**
	 * Returns the filtered children (children without those who don't have children) that will be displayed
	 * under the given Match.
	 * 
	 * @param match
	 *            the given Match.
	 * @return an iterable of the filtered children (children without those who don't have children) that will
	 *         be displayed under the given Match.
	 * @since 3.0
	 */
	public Iterable<?> getFilteredChildren(Match match) {
		ImmutableSet<EObject> containementDifferenceValues = containmentReferencesValues(match);

		@SuppressWarnings("unchecked")
		Predicate<Object> childrenFilter = not(or(matchOfContainmentDiff(containementDifferenceValues),
				matchWithNoChildren(), emptyMatch(), instanceOf(ResourceAttachmentChange.class)));

		Iterable<?> filteredChildren = filter(super.getChildren(match), childrenFilter);
		return filteredChildren;
	}

	/**
	 * Returns the containment references values of the given Match.
	 * 
	 * @param match
	 *            the given Match.
	 * @return the containment references values of the given Match.
	 * @since 3.0
	 */
	public static ImmutableSet<EObject> containmentReferencesValues(Match match) {
		EList<Diff> differences = match.getDifferences();

		Iterable<Diff> containmentReferenceChanges = filter(differences, containmentReferenceChange());

		final Function<Diff, EObject> valueGetter = new Function<Diff, EObject>() {
			public EObject apply(Diff input) {
				return ((ReferenceChange)input).getValue();
			}
		};
		ImmutableSet<EObject> containementDifferenceValues = ImmutableSet.copyOf(transform(
				containmentReferenceChanges, valueGetter));

		return containementDifferenceValues;
	}

	/**
	 * A predicate to know if the given object is a {@link Match} containing a {@link Diff} of type
	 * containment.
	 * 
	 * @param containementDifferenceValues
	 *            the list of containment values.
	 * @return a predicate to know if the given object is a {@link Match} containing a {@link Diff} of type
	 *         containment.
	 * @since 3.0
	 */
	public static Predicate<? super Object> matchOfContainmentDiff(
			final ImmutableSet<? extends EObject> containementDifferenceValues) {
		return new Predicate<Object>() {
			public boolean apply(Object input) {
				boolean ret = false;
				if (input instanceof Match) {
					Match match = (Match)input;
					if (containementDifferenceValues.contains(match.getLeft())
							|| containementDifferenceValues.contains(match.getRight())
							|| containementDifferenceValues.contains(match.getOrigin())) {
						ret = true;
					}
				}
				return ret;
			}
		};
	}

	/**
	 * A predicate to know if the given object is a {@link Match} with no children.
	 * 
	 * @return A predicate to know if the given object is a {@link Match} with no children.
	 */
	private Predicate<? super Object> matchWithNoChildren() {
		return new Predicate<Object>() {
			public boolean apply(Object input) {
				boolean ret = false;
				if (input instanceof Match) {
					Match match = (Match)input;
					ret = Iterables.isEmpty(MatchItemProviderSpec.this.getFilteredChildren(match));
				}
				return ret;
			}
		};
	}

	/**
	 * A predicate to know if the given object is an empty match (no left, right and origin).
	 * 
	 * @return A predicate to know if the given object is an empty match (no left, right and origin).
	 */
	private static Predicate<? super Object> emptyMatch() {
		return new Predicate<Object>() {
			public boolean apply(Object input) {
				if (input instanceof Match) {
					final Match match = (Match)input;
					return match.getLeft() == null && match.getRight() == null && match.getOrigin() == null;
				}
				return false;
			}
		};
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.edit.provider.ItemProviderAdapter#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object object) {
		Match match = (Match)object;
		return !isEmpty(getChildrenIterable(match));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.provider.IItemStyledLabelProvider#getStyledText(java.lang.Object)
	 * @since 3.0
	 */
	public IStyledString.IComposedStyledString getStyledText(Object object) {
		Match match = (Match)object;
		ComposedStyledString styledString = new ComposedStyledString();
		if (any(match.getAllDifferences(), hasState(DifferenceState.UNRESOLVED))) {
			styledString.append("> ", Style.DECORATIONS_STYLER); //$NON-NLS-1$
		}
		styledString.append(getText(object));
		return styledString;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.emf.compare.provider.IItemDescriptionProvider#getDescription(java.lang.Object)
	 */
	public String getDescription(Object object) {
		return getText(object);
	}
}
