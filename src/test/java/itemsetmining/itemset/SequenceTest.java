package itemsetmining.itemset;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;

import org.junit.Test;

public class SequenceTest {

	@Test
	public void testSequenceContains() {

		// Example from Agrawal paper
		final Sequence seq1 = new Sequence(3, 4, 5, 8);
		final Sequence seq2 = new Sequence(7, 3, 8, 9, 4, 5, 6, 8);
		final Sequence seq3 = new Sequence(3, 3, 8);

		assertEquals(true, seq2.contains(seq1));
		assertEquals(false, seq1.contains(seq2));
		assertEquals(false, seq1.contains(seq3));

	}

	@Test
	public void testSequenceGetCovered() {

		final Sequence trans = new Sequence(7, 3, 8, 9, 4, 5, 6, 8);

		final Sequence seq1 = new Sequence(3, 4, 5, 8);
		final BitSet expected1 = new BitSet(trans.size());
		expected1.set(1);
		expected1.set(4);
		expected1.set(5);
		expected1.set(7);

		final Sequence seq2 = new Sequence(7, 9);
		final BitSet expected2 = new BitSet(trans.size());
		expected2.set(0);
		expected2.set(3);

		final Sequence seq3 = new Sequence(8, 4, 5);
		final BitSet expected3 = new BitSet(trans.size());
		expected3.set(2);
		expected3.set(4);
		expected3.set(5);

		// Seq not contained in trans
		final Sequence seq4 = new Sequence(3, 3, 8);
		final BitSet expected4 = new BitSet(trans.size());

		assertEquals(expected1, trans.getCovered(seq1, new BitSet()));
		assertEquals(expected2, trans.getCovered(seq2, new BitSet()));
		assertEquals(expected2, trans.getCovered(seq2, expected1));
		assertEquals(expected3, trans.getCovered(seq3, new BitSet()));
		assertEquals(expected3, trans.getCovered(seq3, expected2));
		assertEquals(expected4, trans.getCovered(seq4, new BitSet()));

		// Test double covering
		final Sequence transC = new Sequence(1, 2, 1, 2, 1, 2);
		final Sequence seqC = new Sequence(1, 2);
		final BitSet expectedC1 = new BitSet(transC.size());
		expectedC1.set(0);
		expectedC1.set(1);
		final BitSet expectedC2 = new BitSet(transC.size());
		expectedC2.set(2);
		expectedC2.set(3);
		final BitSet expectedC3 = new BitSet(transC.size());
		expectedC3.set(4);
		expectedC3.set(5);

		assertEquals(expectedC1, transC.getCovered(seqC, new BitSet()));
		assertEquals(expectedC2, transC.getCovered(seqC, expectedC1));
		expectedC2.or(expectedC1);
		assertEquals(expectedC3, transC.getCovered(seqC, expectedC2));

	}

	@Test
	public void testSequenceJoin() {

		final Sequence seq1 = new Sequence(0, 1, 2, 3);
		final Sequence seq2 = new Sequence(2, 3, 4, 5);

		final Sequence expectedSeq = new Sequence(0, 1, 2, 3, 4, 5);
		assertEquals(expectedSeq, Sequence.join(seq1, seq2));
		assertEquals(null, Sequence.join(seq2, seq1));

	}
}
