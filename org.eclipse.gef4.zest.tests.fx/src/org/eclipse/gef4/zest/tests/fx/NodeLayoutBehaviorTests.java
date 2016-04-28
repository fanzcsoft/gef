/*******************************************************************************
 * Copyright (c) 2014, 2015 itemis AG and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthias Wienand (itemis AG) - initial API & implementation
 *
 *******************************************************************************/
package org.eclipse.gef4.zest.tests.fx;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.eclipse.gef4.geometry.convert.fx.Geometry2FX;
import org.eclipse.gef4.geometry.planar.Dimension;
import org.eclipse.gef4.geometry.planar.Point;
import org.eclipse.gef4.graph.Graph;
import org.eclipse.gef4.graph.Node;
import org.eclipse.gef4.layout.LayoutContext;
import org.eclipse.gef4.layout.LayoutProperties;
import org.eclipse.gef4.mvc.fx.parts.FXRootPart;
import org.eclipse.gef4.mvc.fx.policies.FXResizePolicy;
import org.eclipse.gef4.mvc.fx.policies.FXTransformPolicy;
import org.eclipse.gef4.mvc.fx.providers.FXTransformProvider;
import org.eclipse.gef4.mvc.fx.viewer.FXViewer;
import org.eclipse.gef4.mvc.tests.fx.rules.FXApplicationThreadRule;
import org.eclipse.gef4.zest.fx.behaviors.NodeLayoutBehavior;
import org.eclipse.gef4.zest.fx.parts.NodePart;
import org.junit.Rule;
import org.junit.Test;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.transform.Affine;

public class NodeLayoutBehaviorTests {

	/**
	 * Ensure all tests are executed on the JavaFX application thread (and the
	 * JavaFX toolkit is properly initialized).
	 */
	@Rule
	public FXApplicationThreadRule fxApplicationThreadRule = new FXApplicationThreadRule();

	private Node createNode() {
		Node node = new Node.Builder().buildNode();
		Graph graph = new Graph.Builder().nodes(node).build();
		LayoutContext glc = new LayoutContext();
		glc.setGraph(graph);
		return node;
	}

	private NodeLayoutBehavior createNodeLayoutBehavior(final Point location, final Dimension size, final Node node) {
		NodeLayoutBehavior behavior = new NodeLayoutBehavior() {
			private NodePart host;

			@Override
			public NodePart getHost() {
				if (host == null) {
					host = new NodePart() {
						{
							setAdapter(new FXResizePolicy());
							FXTransformProvider transformProvider = new FXTransformProvider();
							setAdapter(transformProvider, FXTransformPolicy.TRANSFORM_PROVIDER_KEY.getRole());
							setAdapter(new FXTransformPolicy());
							Affine affine = transformProvider.get();
							affine.setTx(location.x);
							affine.setTy(location.y);
						}

						@Override
						protected Group createVisual() {
							Group visual = super.createVisual();
							if (size != null) {
								// ensure we are resizable
								// getNestedChildrenPane().setPrefWidth(10);
								visual.resize(size.width, size.height);
							}
							return visual;
						}

						@Override
						public Node getContent() {
							return node;
						}
					};
					FXRootPart rootPart = new FXRootPart();
					FXViewer viewer = new FXViewer();
					viewer.setAdapter(rootPart);
					host.setParent(rootPart);
				}
				return host;
			}

		};
		return behavior;
	}

	@Test
	public void test_adapt() throws Exception {
		Node nodeLayout = createNode();
		NodeLayoutBehavior behavior = createNodeLayoutBehavior(new Point(), null, nodeLayout);

		Point location = new Point(1, 5);
		Dimension size = new Dimension(100, 200);
		LayoutProperties.setLocation(nodeLayout, location);
		LayoutProperties.setSize(nodeLayout, size);

		// postLayout
		Method method = NodeLayoutBehavior.class.getDeclaredMethod("postLayout", new Class[] {});
		method.setAccessible(true);
		method.invoke(behavior, new Object[] {});

		// zest position is top-left, while layout location is center
		Affine affine = Geometry2FX
				.toFXAffine(behavior.getHost().getAdapter(FXTransformPolicy.class).getCurrentTransform());
		assertEquals(location.getTranslated(size.getScaled(-0.5)),
				new Point(affine.getTx(), affine.getTy()));
	}

	@Test
	public void test_provide() throws Exception {
		final Point location = new Point(10, 20);

		// setup with non-resizable figure
		Node nodeLayout = createNode();
		NodeLayoutBehavior behavior = createNodeLayoutBehavior(location, null, nodeLayout);
		Group visual = behavior.getHost().getVisual();

		// preLayout
		Method method = NodeLayoutBehavior.class.getDeclaredMethod("preLayout", new Class[] {});
		method.setAccessible(true);
		method.invoke(behavior, new Object[] {});

		assertEquals(visual.isResizable(), LayoutProperties.isResizable(nodeLayout));

		// zest position is top-left, while layout location is center
		Bounds layoutBounds = visual.getLayoutBounds();
		double minX = layoutBounds.getMinX();
		double minY = layoutBounds.getMinY();
		double maxX = layoutBounds.getMaxX();
		double maxY = layoutBounds.getMaxY();
		assertEquals(location, LayoutProperties.getLocation(nodeLayout).translate(-minX - ((maxX - minX) / 2),
				-minY - ((maxY - minY) / 2)));
		assertEquals(new Dimension(layoutBounds.getWidth(), layoutBounds.getHeight()),
				LayoutProperties.getSize(nodeLayout));
	}

}
