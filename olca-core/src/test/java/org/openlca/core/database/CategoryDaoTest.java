package org.openlca.core.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openlca.core.Tests;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;

public class CategoryDaoTest {

	private CategoryDao dao = new CategoryDao(Tests.getDb());

	@Test
	public void testSimple() {
		Category category = dao.insert(create());
		Tests.emptyCache();
		Category alias = dao.getForId(category.id);
		assertEquals(category.name, alias.name);
		dao.delete(category); // non-attached
		alias = dao.getForId(category.id);
		Assert.assertNull(alias);
	}

	@Test
	public void testAddChild() {
		Category parent = dao.insert(create());
		Category child = create();
		parent.childCategories.add(child);
		child.category = parent;
		parent = dao.update(parent);
		child = parent.childCategories.get(0);
		Tests.emptyCache();
		Category alias = dao.getForId(parent.id);
		assertEquals(1, alias.childCategories.size());
		assertEquals(child.refId, alias.childCategories.get(0).refId);
		dao.delete(alias);
		Assert.assertNull(dao.getForId(child.id));
	}

	@Test
	public void testSync() {
		dao.sync(ModelType.ACTOR, "some", "actor");
		Category c = dao.sync(ModelType.ACTOR, "some", "actor", "category");
		assertEquals("category", c.name);
		assertNotNull(c.refId);
		assertEquals("actor", c.category.name);
		assertNotNull(c.category.refId);
		assertEquals(c.category.childCategories.size(), 1);
		assertEquals("some", c.category.category.name);
		assertNotNull(c.category.category.refId);
		assertEquals(c.category.category.childCategories.size(), 1);
		assertNull(c.category.category.category);
	}

	@Test
	public void testFindRoot() {
		Category parent = create();
		Category child = create();
		parent.childCategories.add(child);
		child.category = parent;
		dao.insert(parent);
		Tests.emptyCache();
		List<Category> roots = dao.getRootCategories(ModelType.FLOW);
		Assert.assertTrue(roots.contains(parent));
		Assert.assertFalse(roots.contains(child));
		dao.delete(parent);
	}

	@Test
	public void findAllRootTypes() {
		// in the openLCA application not all of these types are really used
		// in categories, but this test should work
		for (ModelType type : ModelType.values()) {
			Category cat = create();
			cat.modelType = type;
			dao.insert(cat);
			Tests.emptyCache();
			List<Category> categories = dao.getRootCategories(type);
			Assert.assertTrue(categories.contains(cat));
			dao.delete(cat);
			categories = dao.getRootCategories(type);
			Assert.assertFalse(categories.contains(cat));
		}
	}

	private Category create() {
		Category category = new Category();
		category.name = "name";
		category.modelType = ModelType.FLOW;
		return category;
	}
}
