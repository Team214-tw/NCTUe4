package com.team214.nycue4.course

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter


class FolderViewPagerAdapter(
    fm: FragmentManager, private val fragments: List<Fragment>,
    private val titles: List<String>
) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return fragments[position]
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getPageTitle(position: Int): CharSequence {
        return titles[position]
    }
}